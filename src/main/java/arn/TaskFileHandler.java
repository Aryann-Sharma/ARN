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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
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
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) {
                        continue;
                    }
                    if (line.equals(DATA_HEADER) && !hasContent) {
                        hasContent = true;
                        continue;
                    }
                    if (line.startsWith("# Arn data")) {
                        throw new StorageException("Unsupported or misplaced save format header at line "
                                + lineNumber + ". Check the save file before restarting Arn.");
                    }
                    hasContent = true;

                    Task task = parseTask(line);
                    if (task == null) {
                        throw new StorageException("Invalid saved task at line " + lineNumber
                                + ". Check the save file before restarting Arn.");
                    }
                    tasks.add(task);
                }
            }
            lastSavedData = savedData;
            hasLoadedData = true;
            return tasks;
        } catch (CharacterCodingException e) {
            throw new StorageException("Saved tasks contain invalid UTF-8 text. "
                    + "Check the save file before restarting Arn.", e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to read task data from " + filePath, e);
            throw new StorageException("Could not read saved tasks.", e);
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
            createParentDirectory();
            Path lockPath = filePath.resolveSibling(filePath.getFileName() + ".lock");
            try (FileChannel lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock lock = lockChannel.tryLock()) {
                if (lock == null) {
                    throw new StorageException("Another instance is saving tasks. Please try again.");
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
            throw new StorageException("Another instance is saving tasks. Please try again.", e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to write task data to " + filePath, e);
            throw new StorageException("Could not save tasks.", e);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
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
            throw new StorageException("Load saved tasks before replacing an existing save file.");
        }
        if (hasLoadedData && !Arrays.equals(lastSavedData, savedData)) {
            throw new StorageException("Saved tasks changed outside this session. "
                    + "Restart Arn to load the latest tasks before making changes.");
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

    private Task parseTask(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            return null;
        }

        String taskType = parts[0].trim();
        String status = parts[1].trim();
        if (!status.equals("0") && !status.equals("1")) {
            return null;
        }

        try {
            Task task = createTask(taskType, parts);
            if (task == null || task.getDescription().isEmpty()) {
                return null;
            }
            if (status.equals("1")) {
                task.markAsDone();
            }
            return task;
        } catch (ArnException | IllegalArgumentException e) {
            return null;
        }
    }

    private Task createTask(String taskType, String[] parts) throws ArnException {
        if (taskType.equals("T") && parts.length >= 3) {
            return new Todo(joinParts(parts, 2, parts.length));
        }
        if (taskType.equals("D") && parts.length >= 4) {
            return new Deadline(joinParts(parts, 2, parts.length - 1),
                    parts[parts.length - 1].trim());
        }
        if (taskType.equals("E") && parts.length >= 5) {
            return new Event(joinParts(parts, 2, parts.length - 2),
                    parts[parts.length - 2].trim(), parts[parts.length - 1].trim());
        }
        return null;
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
        throw new StorageException("Unsupported task type: " + task.getClass().getSimpleName());
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
