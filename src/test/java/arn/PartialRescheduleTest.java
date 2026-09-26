package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PartialRescheduleTest {
    @Test
    public void changingOneEndpointRetainsTheOtherAndItsTime() throws Exception {
        Event original = new Event("Trip", "2026-10-02 1400", "2026-10-04 1600");
        original.markAsDone();

        Event movedStart = original.reschedule("2026-10-03", null);
        assertEquals("2026-10-03 1400", movedStart.formatStartDate(false));
        assertEquals("2026-10-04 1600", movedStart.formatEndDate(false));
        assertTrue(movedStart.isDone());
        assertEquals("Trip", movedStart.getDescription());

        Event movedEnd = original.reschedule(null, "2026-10-05");
        assertEquals("2026-10-02 1400", movedEnd.formatStartDate(false));
        assertEquals("2026-10-05 1600", movedEnd.formatEndDate(false));
        assertEquals("2026-10-02 1400", original.formatStartDate(false));
        assertEquals("2026-10-04 1600", original.formatEndDate(false));
    }

    @Test
    public void bothDatesRetainTheirOwnTimesIncludingMidnight() throws Exception {
        Event original = new Event("Trip", "2026-10-02 0000", "2026-10-04 2359");
        Event updated = original.reschedule("2026-10-05", "2026-10-06");

        assertEquals("2026-10-05 0000", updated.formatStartDate(false));
        assertEquals("2026-10-06 2359", updated.formatEndDate(false));
        assertEquals("[E][ ] Trip (from Oct 5 2026, 12:00AM to Oct 6 2026, 11:59PM)", updated.toString());
    }

    @Test
    public void explicitTimesReplaceOldTimesAndDateOnlyEventsStayDateOnly() throws Exception {
        Event timed = new Event("Trip", "2026-10-02 1400", "2026-10-04 1600");
        Event updated = timed.reschedule("2026-10-03 1500", "2026-10-05");
        assertEquals("2026-10-03 1500", updated.formatStartDate(false));
        assertEquals("2026-10-05 1600", updated.formatEndDate(false));
        Event endUpdated = timed.reschedule(null, "2026-10-04 1800");
        assertEquals("2026-10-02 1400", endUpdated.formatStartDate(false));
        assertEquals("2026-10-04 1800", endUpdated.formatEndDate(false));

        Event dateOnly = new Event("Trip", "2026-10-02", "2026-10-04");
        Event moved = dateOnly.reschedule("2026-10-03", null);
        assertEquals("2026-10-03", moved.formatStartDate(false));
        assertEquals("2026-10-04", moved.formatEndDate(false));
        Event timedAgain = dateOnly.reschedule("2026-10-02 1000", "2026-10-04 1100");
        assertEquals("2026-10-02 1000", timedAgain.formatStartDate(false));
        assertEquals("2026-10-04 1100", timedAgain.formatEndDate(false));
        assertThrows(ArnException.class, () -> dateOnly.reschedule("2026-10-02 1000", null));
        assertThrows(ArnException.class, () -> dateOnly.reschedule(null, "2026-10-04 1100"));
    }

    @Test
    public void retainedTimesAreUsedWhenValidatingTheRange() throws Exception {
        Event overnight = new Event("Shift", "2026-10-02 1800", "2026-10-03 0900");
        assertThrows(ArnException.class, () -> overnight.reschedule("2026-10-03", null));
        assertThrows(ArnException.class, () -> overnight.reschedule(null, "2026-10-02"));
        assertEquals("2026-10-02 1800", overnight.formatStartDate(false));
        Event equal = overnight.reschedule("2026-10-03 0900", null);
        assertEquals(equal.formatStartDate(false), equal.formatEndDate(false));
    }

    @Test
    public void parserKeepsTaskNumbersAndReportsTheFinalRange() throws Exception {
        Event event = new Event("Trip", "2026-10-02 0000", "2026-10-04 1600");
        event.markAsDone();
        TaskList tasks = new TaskList(List.of(new Todo("First"), event, new Todo("Last")));
        Gui gui = new Gui();
        Parser parser = new Parser(tasks, gui);

        parser.parse("reschedule 2 /from 2026-10-03");
        assertEquals("Task rescheduled:\n2. [E][X] Trip (from Oct 3 2026, 12:00AM to Oct 4 2026, 4:00PM)",
                gui.getResponses());
        parser.parse("reschedule\u20032\u2003/to\u20032026-10-05\u2003");
        assertTrue(gui.getResponses().contains("to Oct 5 2026, 4:00PM"));
        assertEquals("First", tasks.get(0).getDescription());
        assertEquals("Last", tasks.get(2).getDescription());
        assertTrue(tasks.get(1).isDone());
        assertEquals(3, tasks.size());
    }

    @Test
    public void invalidClausesAndDatesLeaveTheEventUntouched() throws Exception {
        Event original = new Event("Trip", "2026-10-02 1400", "2026-10-04 1600");
        TaskList tasks = new TaskList(List.of(original));
        Parser parser = new Parser(tasks, new Gui());
        for (String arguments : List.of("", "/from", "/to", "/from /to 2026-10-05", "/from 2026-10-03 /to",
                "/from2026-10-03", "/to2026-10-05", "extra /from 2026-10-03", "/by 2026-10-03",
                "/from 2026-10-03 /from 2026-10-04", "/to 2026-10-05 /to 2026-10-06",
                "/to 2026-10-05 /from 2026-10-03", "/from 2026-10-03 /to2026-10-05",
                "/from 2026-02-30", "/to 2026-10-05 2400", "/from 2026-10-06", "/to 2026-10-01")) {
            assertThrows(ArnException.class, () -> parser.parse("reschedule 1 " + arguments), arguments);
            assertEquals(List.of(original), tasks.getTasks());
            assertEquals("2026-10-02 1400", original.formatStartDate(false));
            assertEquals("2026-10-04 1600", original.formatEndDate(false));
        }
        ArnException rangeError = assertThrows(ArnException.class,
                () -> parser.parse("reschedule 1 /from 2026-10-06"));
        assertTrue(rangeError.getMessage().contains("Omitted endpoints stay unchanged"));
        assertTrue(rangeError.getMessage().contains("date-only updates keep existing times"));
    }

    @Test
    public void partialChangesPersistAndUndoRestoresEachEndpoint(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        arn.getResponse("event Trip /from 2026-10-02 0000 /to 2026-10-04 1600");
        arn.getResponse("mark 1");
        String original = arn.getResponse("list");
        assertFalse(arn.getResponse("reschedule 1 /from 2026-10-03").startsWith("Error:"));
        String movedStart = arn.getResponse("list");
        assertFalse(arn.getResponse("reschedule 1 /to 2026-10-05").startsWith("Error:"));
        assertEquals(arn.getResponse("list"), open(file).getResponse("list"));

        assertTrue(arn.getResponse("undo").startsWith("Undid"));
        assertEquals(movedStart, open(file).getResponse("list"));
        assertTrue(arn.getResponse("undo").startsWith("Undid"));
        assertEquals(original, open(file).getResponse("list"));
    }

    @Test
    public void failedAndUnchangedUpdatesPreserveUndoHistory(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        arn.getResponse("event Trip /from 2026-10-02 0000 /to 2026-10-04 1600");
        String original = arn.getResponse("list");
        String saved = Files.readString(file);
        try (FileChannel channel = FileChannel.open(directory.resolve("arn.txt.lock"), StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            for (String suffix : List.of("/from 2026-10-03", "/to 2026-10-05")) {
                assertTrue(arn.getResponse("reschedule 1 " + suffix).startsWith("Error:"));
                assertEquals(original, arn.getResponse("list"));
                assertEquals(saved, Files.readString(file));
            }
            String unchanged = arn.getResponse("reschedule 1 /from 2026-10-02");
            assertTrue(unchanged.startsWith("The event dates are unchanged:"));
            assertTrue(unchanged.contains("12:00AM"));
            assertFalse(arn.getResponse("reschedule 1 /to 2026-10-04").startsWith("Error:"));
            assertFalse(arn.getResponse("reschedule 1 /from 2026-10-02 /to 2026-10-04").startsWith("Error:"));
        }
        assertTrue(arn.getResponse("reschedule 1 /from 2026-10-06").startsWith("Error:"));
        assertTrue(arn.getResponse("undo").startsWith("Undid"));
        assertEquals(0, arn.getTaskCount());
    }

    private Arn open(Path file) throws StorageException {
        Arn arn = new Arn();
        arn.initialize(new TaskFileHandler(file));
        return arn;
    }
}
