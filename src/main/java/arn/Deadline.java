package arn;

import java.time.LocalDateTime;

/**
 * Represents a type of task that has
 * a description and a due date.
 */

public final class Deadline extends Task {
    private final TaskDate date;

    /**
     * Constructs a Deadline with the given description and due date.
     * Accepts formats: yyyy-MM-dd or yyyy-MM-dd HHmm.
     *
     * @param description description of the task
     * @param date due date in string format
     * @throws ArnException if the date format is invalid
     */
    public Deadline(String description, String date) throws ArnException {
        super(description);
        this.date = TaskDate.parse(date);
    }

    public String getType() {
        return "D";
    }


    /**
     * Formats the date either in a user-friendly format
     * or in a storage format.
     *
     * @param pretty true for human-readable format, false for storage format
     * @return formatted date as a string
     */
    public String formatDate(boolean pretty) {
        return date.format(pretty);
    }

    @Override
    public LocalDateTime getDate() {
        return date.getValue();
    }

    @Override
    public String toString() {
        return "[" + this.getType() + "][" + this.getStatusIcon() + "] " + getDescription()
                + " (by " + this.formatDate(true) + ")";
    }
}
