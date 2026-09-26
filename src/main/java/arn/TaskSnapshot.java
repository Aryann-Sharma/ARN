package arn;

import java.util.ArrayList;
import java.util.List;

/** Stores task order and completion states for rollback and session undo. */
final class TaskSnapshot {
    private final List<Task> tasks;
    private final List<Boolean> statuses;

    TaskSnapshot(TaskList taskList) {
        tasks = taskList.getTasks();
        statuses = new ArrayList<>();
        for (Task task : tasks) {
            statuses.add(task.isDone());
        }
    }

    boolean matches(TaskList taskList) {
        if (!tasks.equals(taskList.getTasks())) {
            return false;
        }
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).isDone() != statuses.get(i)) {
                return false;
            }
        }
        return true;
    }

    TaskList restore() {
        // Descriptions and dates are immutable; edits replace the whole task.
        // Completion is the only field that needs restoring on the saved objects.
        for (int i = 0; i < tasks.size(); i++) {
            if (statuses.get(i)) {
                tasks.get(i).markAsDone();
            } else {
                tasks.get(i).markAsNotDone();
            }
        }
        return new TaskList(tasks);
    }
}
