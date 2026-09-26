package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class DeadlineTest {
    @Test
    public void testValidDeadlineWithDateTime() throws ArnException {
        Deadline d = new Deadline("submit report", "2025-10-01 1800");
        assertTrue(d.toString().contains("submit report"));
        assertTrue(d.toString().contains("Oct")); // formatted month
        assertEquals("D", d.getType());
    }

    @Test
    public void testValidDeadlineWithDateOnly() throws ArnException {
        Deadline d = new Deadline("exam", "2025-10-01");
        assertFalse(d.formatDate(true).contains(":")); // no time when date only
    }

    @Test
    public void testInvalidDeadlineThrowsException() {
        assertThrows(ArnException.class, () -> new Deadline("oops", "2025/10/01"));
    }

    @Test
    public void testImpossibleDatesAndTimesAreRejected() {
        for (String input : List.of("2025-02-29", "2024-02-30", "2026-04-31", "2026-13-01",
                "2026-00-01", "2026-01-00", "2026-10-01 2400", "2026-10-01 1260",
                "2025-02-29 1200", "2026-04-31 1200")) {
            assertThrows(ArnException.class, () -> new Deadline("report", input), input);
        }
    }

    @Test
    public void testDateFormatIsStrict() {
        for (String input : List.of("", "2026-1-01", "2026-01-1", "026-01-01", "+10000-01-01",
                "-0001-01-01", "2026-01-01 900", "2026-01-01 09:00", "2026-01-01T0900",
                "2026-01-01 ", "2026-01-01\n")) {
            assertThrows(ArnException.class, () -> new Deadline("report", input), input);
        }
        assertThrows(ArnException.class, () -> new Deadline("report", null));
    }

    @Test
    public void testLeapDatesAndMidnightPreserveTheirInputFormat() throws ArnException {
        Deadline dateOnly = new Deadline("report", "2024-02-29");
        Deadline midnight = new Deadline("report", "2024-02-29 0000");

        assertEquals(LocalDateTime.of(2024, 2, 29, 0, 0), dateOnly.getDate());
        assertEquals(dateOnly.getDate(), midnight.getDate());
        assertEquals("2024-02-29", dateOnly.formatDate(false));
        assertEquals("2024-02-29 0000", midnight.formatDate(false));
        assertEquals("Feb 29 2024", dateOnly.formatDate(true));
        assertEquals("Feb 29 2024, 12:00AM", midnight.formatDate(true));
    }

    @Test
    public void testPrettyDatesUseTheDocumentedEnglishFormat() throws ArnException {
        Locale original = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE);
            Deadline deadline = new Deadline("report", "2026-10-01 1800");

            assertEquals("Oct 1 2026, 6:00PM", deadline.formatDate(true));
            assertEquals("2026-10-01 1800", deadline.formatDate(false));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original);
        }
    }

    @Test
    public void testFourDigitYearBoundariesRoundTrip() throws ArnException {
        for (String input : List.of("0000-01-01", "0001-01-01", "9999-12-31 2359")) {
            Deadline deadline = new Deadline("report", input);
            assertEquals(input, deadline.formatDate(false));
            assertEquals(deadline.getDate(), new Deadline("report", deadline.formatDate(false)).getDate());
        }
    }

    @Test
    public void testMalformedDateExplainsTheRequiredFormat() {
        for (String input : List.of("2026/10/02", "2026-10-02 09:00", "2026-10-2")) {
            ArnException error = assertThrows(ArnException.class, () -> new Deadline("report", input));
            assertEquals("Deadline due date has an invalid format. Use YYYY-MM-DD or YYYY-MM-DD HHMM, "
                    + "for example 2026-10-02 1800.", error.getMessage());
        }
    }

    @Test
    public void testNonexistentDateIsNotReportedAsAFormatError() {
        for (String input : List.of("2026-02-30", "2026-02-30 0900", "2026-02-30 2400")) {
            ArnException error = assertThrows(ArnException.class, () -> new Deadline("report", input));
            assertEquals("Deadline due date '2026-02-30' does not exist. Check the year, month, and day.",
                    error.getMessage());
        }
    }

    @Test
    public void testInvalidTimeExplainsTheHourAndMinuteLimits() {
        for (String time : List.of("2400", "2360", "1260", "9999")) {
            ArnException error = assertThrows(ArnException.class,
                    () -> new Deadline("report", "2026-10-02 " + time));
            assertEquals("Deadline due time '" + time
                    + "' is invalid. Use HHMM with hours 00-23 and minutes 00-59.", error.getMessage());
        }
    }

    @Test
    public void testMissingDueDateIsIdentified() {
        for (String input : new String[]{null, "", " \t"}) {
            ArnException error = assertThrows(ArnException.class, () -> new Deadline("report", input));
            assertEquals("Deadline due date is required. Use YYYY-MM-DD or YYYY-MM-DD HHMM, "
                    + "for example 2026-10-02 1800.", error.getMessage());
        }
    }
}
