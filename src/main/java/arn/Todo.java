package arn;

/**
 * A type of Task.
 * Each task of Todo type has a description and a status indicating whether it is completed.
 */
public final class Todo extends Task {
    public Todo(String description) {
        super(description);
    }

    public String getType() {
        return "T";
    }

    /**
     * Represents the task as a string in a format that is easy to read for the user
     *
     * @return string representation of task
     */
    @Override
    public String toString() {
        return "[" + this.getType() + "][" + this.getStatusIcon() + "] " + this.getDescription();
    }
}
