package arn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A task date that remembers whether a time was supplied.
 */
final class TaskDate {
    private static final String INVALID_DATE = "Invalid date format. Use YYYY-MM-DD or YYYY-MM-DD HHMM.";
    private static final Pattern INPUT_FORMAT = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}( [0-9]{4})?");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HHmm")
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

    static TaskDate parse(String input) throws ArnException {
        if (input == null || !INPUT_FORMAT.matcher(input).matches()) {
            throw new ArnException(INVALID_DATE);
        }
        try {
            boolean hasTime = input.length() > 10;
            LocalDateTime value = hasTime
                    ? LocalDateTime.parse(input, DATE_TIME_FORMAT)
                    : LocalDate.parse(input, DATE_FORMAT).atStartOfDay();
            return new TaskDate(value, hasTime);
        } catch (DateTimeParseException e) {
            throw new ArnException(INVALID_DATE);
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
