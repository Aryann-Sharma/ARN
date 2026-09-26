package arn;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Represents a task of type Event with a description, a start date, and end date.
 *
 */
public class Event extends Task {
    protected LocalDateTime startDate;
    protected LocalDateTime endDate;
    private boolean hasTime = true;


    /**
     * Constructs an Event with the given description, start date, and end date.
     * Accepts formats: yyyy-MM-dd or yyyy-MM-dd HHmm.
     *
     * @param description description of the event
     * @param startDate start date of the event
     * @param endDate end date of the event
     * @throws ArnException if the date formats are invalid
     */
    public Event(String description, String startDate, String endDate) throws ArnException {
        super(description);
        DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            this.startDate = LocalDateTime.parse(startDate, dateTimeFmt);
            this.endDate = LocalDateTime.parse(endDate, dateTimeFmt);
        } catch (DateTimeParseException e1) {
            try {
                LocalDate d1 = LocalDate.parse(startDate, dateFmt);
                this.startDate = d1.atStartOfDay();
                LocalDate d2 = LocalDate.parse(endDate, dateFmt);
                this.endDate = d2.atStartOfDay();
                this.hasTime = false;
            } catch (DateTimeParseException e2) {
                throw new ArnException("Invalid date format. Use yyyy-mm-dd or yyyy-mm-dd hhmm.");
            }
        }

        if (this.endDate.isBefore(this.startDate)) {
            throw new ArnException("Event end date must not be before its start date.");
        }
    }

    public String getType() {
        return "E";
    }

    public String formatStartDate(boolean pretty) {
        if (pretty) {
            return hasTime
                    ? startDate.format(DateTimeFormatter.ofPattern("MMM d yyyy, h:mma"))
                    : startDate.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d yyyy"));
        } else {
            return hasTime
                    ? startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"))
                    : startDate.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    public String formatEndDate(boolean pretty) {
        if (pretty) {
            return hasTime
                    ? endDate.format(DateTimeFormatter.ofPattern("MMM d yyyy, h:mma"))
                    : endDate.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d yyyy"));
        } else {
            return hasTime
                    ? endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"))
                    : endDate.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    @Override
    public LocalDateTime getDate() {
        return this.startDate;
    }

    @Override
    public String toString() {
        return "[" + this.getType() + "][" + this.getStatusIcon() + "] " + description + " (from " +
                this.formatStartDate(true) + " to " + this.formatEndDate(true) + ")";
    }
}
