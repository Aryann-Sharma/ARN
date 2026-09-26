package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}

