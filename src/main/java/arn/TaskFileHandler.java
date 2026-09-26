package arn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes task data using a small, versioned text format.
 */
public final class TaskFileHandler {
    static final String DATA_HEADER = "# Arn data v1";

    private static final Logger LOGGER = Logger.getLogger(TaskFileHandler.class.getName());

    private final Path filePath;
    private byte[] lastSavedData;
    private boolean hasLoadedData;

    public TaskFileHandler(String filePath) {
        this(Path.of(filePath));
    }

    public TaskFileHandler(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath").toAbsolutePath().normalize();
    }

    /**
     * Loads tasks, including legacy files without a version header.
     * Rejects malformed data so a later save cannot silently remove unread tasks.
     *
     * @return tasks loaded from storage
     * @throws StorageException if the file cannot be read or contains invalid data
     */
    public synchronized List<Task> readTasks() throws StorageException {
        List<Task> tasks = new ArrayList<>();
        try {
            checkSavePath(false);
            byte[] savedData = readSavedData();
            if (savedData == null) {
                lastSavedData = null;
                hasLoadedData = true;
                return tasks;
            }

            String contents = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(savedData)).toString();
            if (contents.startsWith("\uFEFF")) {
                contents = contents.substring(1);
            }
            try (BufferedReader reader = new BufferedReader(new StringReader(contents))) {
                String line;
                int lineNumber = 0;
                boolean hasContent = false;
                boolean hasHeader = false;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) {
                        continue;
                    }
                    if (line.equals(DATA_HEADER)) {
                        if (hasContent) {
                            String problem = hasHeader ? "Duplicate" : "Misplaced";
                            throw new StorageException(problem + " save format header at line " + lineNumber
                                    + " in " + quotedPath() + ". The header must appear once, before any tasks. "
                                    + "Back up the file, correct the header placement, then restart Arn.");
                        }
                        hasContent = true;
                        hasHeader = true;
                        continue;
                    }
                    if (line.startsWith("# Arn data")) {
                        throw new StorageException("Unsupported save format header at line " + lineNumber
                                + " in " + quotedPath() + ": \"" + line + "\". This version supports \""
                                + DATA_HEADER + "\" and unversioned files. "
                                + "Use an Arn version that supports this format or restore a compatible backup.");
                    }
                    hasContent = true;

                    try {
                        tasks.add(parseTask(line));
                    } catch (ArnException | IllegalArgumentException e) {
                        throw new StorageException("Invalid saved task at line " + lineNumber + " in "
                                + quotedPath() + ": " + e.getMessage() + " Back up the file, correct this line "
                                + "or restore a valid backup, then restart Arn.", e);
                    }
                }
            }
            lastSavedData = savedData;
            hasLoadedData = true;
            return tasks;
        } catch (CharacterCodingException e) {
            throw new StorageException("Could not read saved tasks from " + quotedPath()
                    + ": the file contains invalid UTF-8 text. Restore a valid UTF-8 backup, "
                    + "or back up this file and save a corrected UTF-8 copy, then restart Arn.", e);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Unable to read task data from " + filePath, e);
            throw storageFailure(false, e);
        }
    }

    /**
     * Writes tasks to a temporary file before replacing the active save file.
     * A lock and a comparison with the last loaded data prevent concurrent Arn
     * instances from overwriting each other's changes.
     *
     * @param tasks tasks to persist
     * @throws StorageException if the data cannot be written safely
     */
    public synchronized void writeTasks(List<Task> tasks) throws StorageException {
        Objects.requireNonNull(tasks, "tasks");
        Path temporaryFile = null;
        try {
            byte[] newData = serializeTasks(tasks);
            checkSavePath(true);
            createParentDirectory();
            Path lockPath = filePath.resolveSibling(filePath.getFileName() + ".lock");
            if (Files.isDirectory(lockPath)) {
                throw new StorageException("Could not save tasks to " + quotedPath() + ": the lock path \""
                        + lockPath + "\" is a directory. Move or rename that directory so Arn can create "
                        + "its lock file, then retry.");
            }
            try (FileChannel lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock lock = lockChannel.tryLock()) {
                if (lock == null) {
                    throw new StorageException(lockConflictMessage());
                }
                checkForExternalChanges(readSavedData());
                temporaryFile = Files.createTempFile(filePath.getParent(), "arn-", ".tmp");
                try (FileChannel writer = FileChannel.open(temporaryFile, StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = ByteBuffer.wrap(newData);
                    while (buffer.hasRemaining()) {
                        writer.write(buffer);
                    }
                    writer.force(true);
                }
                checkForExternalChanges(readSavedData());
                replaceSaveFile(temporaryFile);
                temporaryFile = null;
                lastSavedData = newData;
                hasLoadedData = true;
            }
        } catch (OverlappingFileLockException e) {
            throw new StorageException(lockConflictMessage(), e);
        } catch (CharacterCodingException e) {
            throw new StorageException("Could not save tasks to " + quotedPath()
                    + ": a task description contains invalid Unicode text. Retype the description and retry.", e);
        } catch (ReadOnlyFileSystemException e) {
            throw new StorageException("Could not save tasks to " + quotedPath()
                    + ": the filesystem is read-only. Copy Arn and its data folder to a writable location, "
                    + "or make this filesystem writable, then restart Arn.", e);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Unable to write task data to " + filePath, e);
            throw storageFailure(true, e);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    private String quotedPath() {
        return "\"" + filePath + "\"";
    }

    private String lockConflictMessage() {
        return "Another instance is saving tasks to " + quotedPath()
                + ". Wait for it to finish and retry. If it remains busy, close the other Arn instance first.";
    }

    private void checkSavePath(boolean writing) throws StorageException {
        String operation = writing ? "save tasks to " : "read saved tasks from ";
        if (Files.isDirectory(filePath)) {
            throw new StorageException("Could not " + operation + quotedPath()
                    + ": this path is a directory, but Arn needs a file. "
                    + "Move or rename that directory, then restart Arn.");
        }
        for (Path parent = filePath.getParent(); parent != null; parent = parent.getParent()) {
            if (Files.exists(parent) && !Files.isDirectory(parent)) {
                throw new StorageException("Could not " + operation + quotedPath() + ": parent path \""
                        + parent + "\" is not a directory. Move or rename the blocking file so Arn "
                        + "can use that folder, then restart Arn.");
            }
        }
    }

    private StorageException storageFailure(boolean writing, IOException cause) {
        String operation = writing ? "save tasks to " : "read saved tasks from ";
        String detail;
        if (cause instanceof AccessDeniedException) {
            detail = "Access was denied. Check permissions for the save file and its folder, "
                    + "and whether another program is blocking access, then "
                    + (writing ? "retry." : "restart Arn.");
        } else {
            String reason = cause instanceof FileSystemException filesystemError
                    ? filesystemError.getReason() : cause.getMessage();
            detail = reason == null || reason.isBlank()
                    ? "The filesystem could not complete the operation. "
                    : "The filesystem reported: " + reason.strip() + ". ";
            detail += writing
                    ? "Check that the drive is connected, the folder is writable, and there is enough free space, "
                            + "then retry."
                    : "Check that the drive is connected and the file and its folder are readable, then restart Arn.";
        }
        if (cause instanceof FileSystemException filesystemError && filesystemError.getFile() != null
                && !filesystemError.getFile().equals(filePath.toString())) {
            detail += " Affected path: \"" + filesystemError.getFile() + "\".";
        }
        return new StorageException("Could not " + operation + quotedPath() + ". " + detail, cause);
    }

    private byte[] readSavedData() throws IOException {
        try {
            return Files.readAllBytes(filePath);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    private void checkForExternalChanges(byte[] savedData) throws StorageException {
        if (!hasLoadedData && savedData != null && savedData.length > 0) {
            throw new StorageException("Could not save tasks to " + quotedPath()
                    + ": the existing file has not been loaded successfully. Restart Arn to load it; "
                    + "if loading fails, repair or restore the file before retrying.");
        }
        if (hasLoadedData && !Arrays.equals(lastSavedData, savedData)) {
            if (savedData == null) {
                throw new StorageException("The save file " + quotedPath()
                        + " was removed or moved after this session loaded it. Restore the file if you want "
                        + "to keep its tasks, then restart Arn before making changes.");
            }
            throw new StorageException("Saved tasks in " + quotedPath() + " changed outside this session. "
                    + "Restart Arn to load the latest saved tasks before retrying this command.");
        }
    }

    private byte[] serializeTasks(List<Task> tasks) throws IOException, StorageException {
        StringBuilder contents = new StringBuilder(DATA_HEADER).append('\n');
        for (Task task : tasks) {
            contents.append(serializeTask(task)).append('\n');
        }
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(contents));
        byte[] data = new byte[encoded.remaining()];
        encoded.get(data);
        return data;
    }

    private Task parseTask(String line) throws ArnException {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            throw new ArnException("Expected TYPE | STATUS | DESCRIPTION, with date fields for deadlines and events.");
        }

        String taskType = parts[0].trim();
        String status = parts[1].trim();
        if (!status.equals("0") && !status.equals("1")) {
            throw new ArnException("Unknown task status \"" + status
                    + "\". Use 0 for incomplete or 1 for complete.");
        }

        Task task = createTask(taskType, parts);
        if (status.equals("1")) {
            task.markAsDone();
        }
        return task;
    }

    private Task createTask(String taskType, String[] parts) throws ArnException {
        if (taskType.equals("T") && parts.length >= 3) {
            return new Todo(joinParts(parts, 2, parts.length));
        }
        if (taskType.equals("D")) {
            if (parts.length < 4) {
                throw new ArnException("The deadline is missing its due date field. "
                        + "Expected D | STATUS | DESCRIPTION | DUE DATE.");
            }
            return new Deadline(joinParts(parts, 2, parts.length - 1),
                    parts[parts.length - 1].trim());
        }
        if (taskType.equals("E")) {
            if (parts.length < 5) {
                throw new ArnException("The event is missing a start or end date field. "
                        + "Expected E | STATUS | DESCRIPTION | START DATE | END DATE.");
            }
            return new Event(joinParts(parts, 2, parts.length - 2),
                    parts[parts.length - 2].trim(), parts[parts.length - 1].trim());
        }
        throw new ArnException("Unknown task type \"" + taskType
                + "\". Use T for todo, D for deadline, or E for event.");
    }

    private String serializeTask(Task task) throws StorageException {
        Objects.requireNonNull(task, "task");
        String status = task.isDone() ? "1" : "0";
        if (task instanceof Todo) {
            return "T | " + status + " | " + task.getDescription();
        }
        if (task instanceof Deadline deadline) {
            return "D | " + status + " | " + deadline.getDescription()
                    + " | " + deadline.formatDate(false);
        }
        if (task instanceof Event event) {
            return "E | " + status + " | " + event.getDescription()
                    + " | " + event.formatStartDate(false)
                    + " | " + event.formatEndDate(false);
        }
        throw new StorageException("Could not save tasks to " + quotedPath() + ": task type \""
                + task.getClass().getSimpleName() + "\" is not supported. Use a todo, deadline, or event.");
    }

    private String joinParts(String[] parts, int start, int end) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                result.append("|");
            }
            result.append(parts[i]);
        }
        return result.toString().strip();
    }

    private void createParentDirectory() throws IOException {
        Path parentDirectory = filePath.getParent();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }
    }

    private void replaceSaveFile(Path temporaryFile) throws IOException {
        try {
            Files.move(temporaryFile, filePath,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Unable to remove temporary task file " + temporaryFile, e);
        }
    }
}
