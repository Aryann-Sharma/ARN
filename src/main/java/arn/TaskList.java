package arn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a list of tasks.
 */

public final class TaskList {
    private final List<Task> taskList;

    /**
     * Constructs a TaskList with an initial set of tasks.
     *
     * @param taskList the list of tasks to initialize with
     */
    public TaskList(List<Task> taskList) {
        this.taskList = new ArrayList<>(List.copyOf(Objects.requireNonNull(taskList, "taskList")));
    }

    public List<Task> getTasks() {
        return List.copyOf(taskList);
    }

    /**
     * Retrieves a task at the specified index.
     *
     * @param index the index of the task to retrieve
     * @throws ArnException if the index is out of bounds
     */
    public Task get(int index) throws ArnException {
        if (index < 0 || index >= taskList.size()) {
            throw new ArnException("Invalid task number.");
        }
        return taskList.get(index);
    }

    /**
     * Adds a new task to the list.
     *
     * @param task the task to add
     */
    public void add(Task task) {
        taskList.add(Objects.requireNonNull(task, "task"));
    }

    /**
     * Removes a task at the specified index.
     *
     * @param index the index of the task to remove
     * @throws ArnException if the index is out of bounds
     */
    public Task remove(int index) throws ArnException {
        if (index < 0 || index >= taskList.size()) {
            throw new ArnException("Invalid task number.");
        }
        return taskList.remove(index);
    }

    /**
     * Returns a list of tasks whose description contains the given keyword.
     *
     * @param keyword the keyword to search for
     * @return a list of matching tasks
     */
    public List<Task> find(String keyword) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        List<Task> matchList = new ArrayList<>();
        for (Task task : taskList) {
            if (task.getDescription().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                matchList.add(task);
            }
        }
        return matchList;
    }

    public List<Task> sortByDate() {
        List<Task> sortList = new ArrayList<>();
        for (Task task : taskList) {
            if (task.getDate() != null) {
                sortList.add(task);
            }
        }

        sortList.sort(new DateComparator());
        return sortList;
    }

    public int size() {
        return taskList.size();
    }
}
