package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class EventTest {
    @Test
    public void testValidEventWithDateTime() throws ArnException {
        Event e = new Event("meeting", "2025-10-01 1000", "2025-10-01 1200");
        assertTrue(e.toString().contains("meeting"));
        assertTrue(e.formatStartDate(true).contains("10:00"));
        assertEquals("E", e.getType());
    }

    @Test
    public void testValidEventWithDateOnly() throws ArnException {
        Event e = new Event("conference", "2025-10-01", "2025-10-03");
        assertFalse(e.formatStartDate(true).contains(":"));
        assertTrue(e.toString().contains("conference"));
    }

    @Test
    public void testInvalidEventThrowsException() {
        assertThrows(ArnException.class, () -> new Event("invalid", "2025/10/01", "2025/10/02"));
    }

    @Test
    public void testEndDateBeforeStartDateThrowsException() {
        assertThrows(ArnException.class,
                () -> new Event("backwards", "2025-10-03", "2025-10-01"));
    }

    @Test
    public void testEndTimeBeforeStartTimeThrowsException() {
        assertThrows(ArnException.class,
                () -> new Event("backwards", "2025-10-01 1400", "2025-10-01 1200"));
    }

    @Test
    public void testInvalidCalendarDatesAndTimesAreRejectedAtBothEndpoints() {
        for (String invalid : List.of("2025-02-29", "2026-04-31", "2026-10-01 2400")) {
            assertThrows(ArnException.class, () -> new Event("meeting", invalid, "2026-12-31"));
            assertThrows(ArnException.class, () -> new Event("meeting", "2024-01-01", invalid));
        }
        assertThrows(ArnException.class, () -> new Event("meeting", null, "2026-10-01"));
        assertThrows(ArnException.class, () -> new Event("meeting", "2026-10-01", null));
    }

    @Test
    public void testMixedDateAndTimeFormatsHaveASpecificError() {
        ArnException error = assertThrows(ArnException.class,
                () -> new Event("meeting", "2026-10-01", "2026-10-01 1200"));
        assertEquals("Event start and end must both include a time or both omit it.", error.getMessage());
        assertThrows(ArnException.class,
                () -> new Event("meeting", "2026-10-01 1200", "2026-10-02"));
    }

    @Test
    public void testEqualEndpointsAndLeapDatesAreSupported() throws ArnException {
        Event dateOnly = new Event("meeting", "2024-02-29", "2024-02-29");
        Event midnight = new Event("meeting", "2024-02-29 0000", "2024-02-29 0000");

        assertEquals("2024-02-29", dateOnly.formatStartDate(false));
        assertEquals("2024-02-29", dateOnly.formatEndDate(false));
        assertEquals("2024-02-29 0000", midnight.formatStartDate(false));
        assertEquals("2024-02-29 0000", midnight.formatEndDate(false));
        assertEquals(dateOnly.getDate(), midnight.getDate());
    }
}
