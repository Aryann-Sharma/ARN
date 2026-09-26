package arn;

import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    private final String description;
    private boolean isDone;

    public Task(String description) {
        Objects.requireNonNull(description, "Task description is required. Add a short description of the task.");
        if (description.isBlank()) {
            throw new IllegalArgumentException("Task description is required. Add a short description of the task.");
        }
        if (description.contains("\n") || description.contains("\r")) {
            throw new IllegalArgumentException("Task description must be on one line. Remove any line breaks.");
        }
        this.description = description.strip();
        this.isDone = false;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return isDone;
    }

    public String getStatusIcon() {
        return (isDone ? "X" : " ");
    }

    public void markAsDone() {
        this.isDone = true;
    }

    public void markAsNotDone() {
        this.isDone = false;
    }

    public LocalDateTime getDate() {
        return null;
    }

    @Override
    public String toString() {
        return "[" + this.getStatusIcon() + "] " + this.description;
    }
}
