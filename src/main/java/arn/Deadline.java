package arn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Represents a type of task that has
 * a description and a due date.
 */

public final class Deadline extends Task {
    private LocalDateTime date;
    private boolean hasTime = true;

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
        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            this.date = LocalDateTime.parse(date, dateTimeFmt);
        } catch (DateTimeParseException e1) {
            try {
                LocalDate d = LocalDate.parse(date, dateFmt);
                this.date = d.atStartOfDay();
                this.hasTime = false;
            } catch (DateTimeParseException e2) {
                throw new ArnException("Invalid date format. Use YYYY-MM-DD or YYYY-MM-DD HHMM.");
            }
        }
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
        if (date == null) {
            return "";
        }
        if (pretty) {
            return hasTime
                    ? date.format(DateTimeFormatter.ofPattern("MMM d yyyy, h:mma"))
                    : date.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d yyyy"));
        } else {
            return hasTime
                    ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"))
                    : date.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    @Override
    public LocalDateTime getDate() {
        return this.date;
    }

    @Override
    public String toString() {
        return "[" + this.getType() + "][" + this.getStatusIcon() + "] " + getDescription() + " (by " +
                this.formatDate(true) + ")";
    }
}
