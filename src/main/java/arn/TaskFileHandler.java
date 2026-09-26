package arn;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Handles reading from and writing to the task save data file.
 * <p>
 * Provides persistent storage so that tasks are saved
 * between runs of the application.
 */
public class TaskFileHandler {
    protected String filePath;

    /**
     * Constructs a TaskFileHandler with the given file path.
     *
     * @param filePath the path to the data file
     */
    public TaskFileHandler(String filePath) {
        this.filePath = filePath;
    }


    /**
     * Reads tasks from the data file.
     *
     * @return the list of tasks loaded from storage
     */
    public ArrayList<Task> readTasks() {
        ArrayList<Task> taskList = new ArrayList<>();
        File file = new File(filePath);
        BufferedReader br = null;
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                System.out.println("(New save file created called arn.txt inside ./data/)");
                System.out.println("");
                return taskList;
            }

            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            while (line != null) {
                Task task = parseTask(line);
                if (task != null) {
                    taskList.add(task);
                } else {
                    System.out.println("Skipped a corrupted line");
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println("Error in reading saved tasks: " + e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Error closing file: " + e.getMessage());
                }
            }
        }

        return taskList;
    }


    /**
     * Parses the given line to convert into task
     *
     * @param line the line to be parsed
     */
    public Task parseTask(String line) {
        assert line != null : "line in file must not be null";
        if (line.isBlank()) {
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
            Task task;
            if (taskType.equals("T") && parts.length >= 3) {
                task = new Todo(joinParts(parts, 2, parts.length));
            } else if (taskType.equals("D") && parts.length >= 4) {
                task = new Deadline(joinParts(parts, 2, parts.length - 1),
                        parts[parts.length - 1].trim());
            } else if (taskType.equals("E") && parts.length >= 5) {
                task = new Event(joinParts(parts, 2, parts.length - 2),
                        parts[parts.length - 2].trim(), parts[parts.length - 1].trim());
            } else {
                return null;
            }

            if (task.description.isEmpty()) {
                return null;
            }
            if (status.equals("1")) {
                task.markAsDone();
            }
            return task;
        } catch (ArnException | RuntimeException e) {
            return null;
        }
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


    /**
     * Writes the given tasks to the data file.
     *
     * @param taskList the tasks to be saved
     */
    public void writeTasks(ArrayList<Task> taskList) {
        assert taskList != null : "task list must not be null";
        File file = new File(filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (Task task: taskList) {
                String line = "";
                if (task instanceof Todo) {
                    line = "T | " + (task.isDone ? "1" : "0") + " | " + task.description;
                } else if (task instanceof Deadline) {
                    Deadline d = (Deadline) task;
                    line = "D | " + (task.isDone ? "1" : "0") + " | " + d.description + " | " + d.formatDate(false);
                } else if (task instanceof Event) {
                    Event e = (Event) task;
                    line = "E | " + (task.isDone ? "1" : "0") + " | " + e.description + " | "
                            + e.formatStartDate(false) + " | " + e.formatEndDate(false);
                }

                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error in writing tasks: " + e.getMessage());
        }
    }
}
