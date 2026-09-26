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
        this(description, TaskDate.parse(startDate, "Event start"), TaskDate.parse(endDate, "Event end"));
    }

    private Event(String description, TaskDate startDate, TaskDate endDate) throws ArnException {
        super(description);
        this.startDate = startDate;
        this.endDate = endDate;
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

    /** Returns an updated event; null keeps an endpoint, and a date alone keeps its time. */
    Event reschedule(String start, String end) throws ArnException {
        TaskDate updatedStart = start == null ? startDate : startDate.reschedule(start, "Event start");
        TaskDate updatedEnd = end == null ? endDate : endDate.reschedule(end, "Event end");
        Event updated = new Event(getDescription(), updatedStart, updatedEnd);
        if (isDone()) {
            updated.markAsDone();
        }
        return updated;
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
