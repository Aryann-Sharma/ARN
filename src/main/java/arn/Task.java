package arn;

import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    private final String description;
    private boolean isDone;

    public Task(String description) {
        this.description = Objects.requireNonNull(description, "description");
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
