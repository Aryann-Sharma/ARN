package arn;

import java.time.LocalDateTime;

/**
 * Represents a task of type Event with a description, a start date, and end date.
 *
 */
public final class Event extends Task {
    private final TaskDate startDate;
    private final TaskDate endDate;


    /**
     * Constructs an Event with the given description, start date, and end date.
     * Accepts formats: yyyy-MM-dd or yyyy-MM-dd HHmm.
     *
     * @param description description of the event
     * @param startDate start date of the event
     * @param endDate end date of the event
     * @throws ArnException if a date is invalid, the formats differ, or the end precedes the start
     */
    public Event(String description, String startDate, String endDate) throws ArnException {
        super(description);
        this.startDate = TaskDate.parse(startDate, "Event start");
        this.endDate = TaskDate.parse(endDate, "Event end");
        if (this.startDate.hasTime() != this.endDate.hasTime()) {
            throw new ArnException("Event start and end use different formats. "
                    + "Include a time in both the start and end, or omit both times.");
        }

        if (this.endDate.getValue().isBefore(this.startDate.getValue())) {
            throw new ArnException("Event end '" + this.endDate.format(false) + "' is before its start '"
                    + this.startDate.format(false) + "'. Set the end to the start or later.");
        }
    }

    public String getType() {
        return "E";
    }

    public String formatStartDate(boolean pretty) {
        return startDate.format(pretty);
    }

    public String formatEndDate(boolean pretty) {
        return endDate.format(pretty);
    }

    @Override
    public LocalDateTime getDate() {
        return startDate.getValue();
    }

    @Override
    public String toString() {
        return "[" + this.getType() + "][" + this.getStatusIcon() + "] " + getDescription()
                + " (from " + this.formatStartDate(true) + " to " + this.formatEndDate(true) + ")";
    }
}
