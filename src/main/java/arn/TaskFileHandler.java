package arn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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

    public TaskFileHandler(String filePath) {
        this(Path.of(filePath));
    }

    public TaskFileHandler(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath").toAbsolutePath().normalize();
    }

    /**
     * Loads all valid tasks. Legacy files without a version header remain supported.
     * Malformed records are skipped and logged with their line number.
     *
     * @return tasks loaded from storage
     * @throws StorageException if the file itself cannot be read
     */
    public List<Task> readTasks() throws StorageException {
        List<Task> tasks = new ArrayList<>();
        try {
            createParentDirectory();
            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
                return tasks;
            }

            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank() || line.equals(DATA_HEADER)) {
                        continue;
                    }

                    Task task = parseTask(line);
                    if (task == null) {
                        LOGGER.warning("Skipped malformed task data at line " + lineNumber);
                    } else {
                        tasks.add(task);
                    }
                }
            }
            return tasks;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to read task data from " + filePath, e);
            throw new StorageException("Could not read saved tasks.", e);
        }
    }

    /**
     * Writes tasks to a temporary file before replacing the active save file.
     *
     * @param tasks tasks to persist
     * @throws StorageException if the data cannot be written safely
     */
    public void writeTasks(List<Task> tasks) throws StorageException {
        Objects.requireNonNull(tasks, "tasks");
        Path temporaryFile = null;
        try {
            createParentDirectory();
            Path parentDirectory = filePath.getParent();
            temporaryFile = Files.createTempFile(parentDirectory, "arn-", ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writer.write(DATA_HEADER);
                writer.newLine();
                for (Task task : tasks) {
                    writer.write(serializeTask(task));
                    writer.newLine();
                }
            }

            replaceSaveFile(temporaryFile);
            temporaryFile = null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to write task data to " + filePath, e);
            throw new StorageException("Could not save tasks.", e);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
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
        } catch (ArnException e) {
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
        return result.toString().trim();
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
