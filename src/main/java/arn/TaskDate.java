package arn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A task date that remembers whether a time was supplied.
 */
final class TaskDate {
    private static final String DATE_USAGE = "Use YYYY-MM-DD or YYYY-MM-DD HHMM, for example 2026-10-02 1800.";
    private static final Pattern INPUT_FORMAT = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}( [0-9]{4})?");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HHmm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter PRETTY_DATE = DateTimeFormatter.ofPattern("MMM d uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter PRETTY_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d uuuu, h:mma", Locale.ENGLISH);

    private final LocalDateTime value;
    private final boolean hasTime;

    private TaskDate(LocalDateTime value, boolean hasTime) {
        this.value = value;
        this.hasTime = hasTime;
    }

    static TaskDate parse(String input, String field) throws ArnException {
        if (input == null || input.isBlank()) {
            throw new ArnException(field + " date is required. " + DATE_USAGE);
        }
        if (!INPUT_FORMAT.matcher(input).matches()) {
            throw new ArnException(field + " date has an invalid format. " + DATE_USAGE);
        }

        String dateText = input.substring(0, 10);
        LocalDate date;
        try {
            date = LocalDate.parse(dateText, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ArnException(field + " date '" + dateText
                    + "' does not exist. Check the year, month, and day.");
        }

        if (input.length() == 10) {
            return new TaskDate(date.atStartOfDay(), false);
        }
        String timeText = input.substring(11);
        try {
            return new TaskDate(date.atTime(LocalTime.parse(timeText, TIME_FORMAT)), true);
        } catch (DateTimeParseException e) {
            throw new ArnException(field + " time '" + timeText
                    + "' is invalid. Use HHMM with hours 00-23 and minutes 00-59.");
        }
    }

    LocalDateTime getValue() {
        return value;
    }

    boolean hasTime() {
        return hasTime;
    }

    String format(boolean pretty) {
        if (pretty) {
            return value.format(hasTime ? PRETTY_DATE_TIME : PRETTY_DATE);
        }
        return value.format(hasTime ? DATE_TIME_FORMAT : DATE_FORMAT);
    }
}
