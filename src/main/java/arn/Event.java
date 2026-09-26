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
     * @throws ArnException if the date formats are invalid
     */
    public Event(String description, String startDate, String endDate) throws ArnException {
        super(description);
        this.startDate = TaskDate.parse(startDate);
        this.endDate = TaskDate.parse(endDate);
        if (this.startDate.hasTime() != this.endDate.hasTime()) {
            throw new ArnException("Event start and end must both include a time or both omit it.");
        }

        if (this.endDate.getValue().isBefore(this.startDate.getValue())) {
            throw new ArnException("Event end date must not be before its start date.");
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
