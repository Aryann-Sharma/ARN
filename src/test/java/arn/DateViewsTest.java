package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class DateViewsTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneId.of("UTC"));

    @Test
    public void todayGroupsOverdueDeadlinesAndKeepsOriginalNumbers() throws Exception {
        Todo todo = new Todo("Read");
        Deadline later = new Deadline("Evening report", "2026-11-01 1800");
        Deadline overdue = new Deadline("Late report", "2026-10-31 2359");
        Event ongoing = new Event("Trip", "2026-10-30", "2026-11-02");
        Deadline morning = new Deadline("Morning report", "2026-11-01 0900");
        TaskList tasks = new TaskList(List.of(todo, later, overdue, ongoing, morning));
        List<Task> original = tasks.getTasks();
        Gui ui = new Gui();
        Parser parser = new Parser(tasks, ui, CLOCK);

        parser.parse("today");

        assertEquals("Today (2026-11-01):\nOverdue deadlines:\n3. " + overdue
                + "\nDue or scheduled today:\n4. " + ongoing + "\n5. " + morning + "\n2. " + later,
                ui.getResponses());
        assertEquals(original, tasks.getTasks());
        parser.parse("mark 5");
        assertTrue(morning.isDone());
        ui.clearResponses();
        parser.parse("today");
        assertFalse(ui.getResponses().contains("Morning report"));
    }

    @Test
    public void calendarDatesIncludeMidnightAndBothEndsOfAnEvent() throws Exception {
        Event endsToday = new Event("Overnight", "2026-10-31 2300", "2026-11-01 0000");
        Event startsToday = new Event("Late trip", "2026-11-01 2359", "2026-11-02 0100");
        Deadline midnight = new Deadline("Midnight", "2026-11-01 0000");
        Deadline allDay = new Deadline("All day", "2026-11-01");
        Event finished = new Event("Yesterday", "2026-10-30", "2026-10-31");
        TaskList tasks = new TaskList(List.of(startsToday, midnight, allDay, endsToday, finished));

        assertEquals(List.of(endsToday, midnight, allDay, startsToday), tasks.tasksBetween(TODAY, TODAY));
        assertEquals(List.of(), tasks.overdueDeadlines(TODAY));
        assertEquals(List.of(startsToday), tasks.tasksBetween(TODAY.plusDays(1), TODAY.plusDays(7)));
    }

    @Test
    public void upcomingUsesExactlySevenDaysAcrossMonthYearAndLeapDayBoundaries() throws Exception {
        for (LocalDate date : List.of(TODAY, LocalDate.of(2026, 12, 28), LocalDate.of(2028, 2, 27))) {
            Deadline today = new Deadline("Today", date.toString());
            Deadline tomorrow = new Deadline("Tomorrow", date.plusDays(1) + " 0000");
            Deadline lastDay = new Deadline("Last day", date.plusDays(7) + " 2359");
            Deadline outside = new Deadline("Outside", date.plusDays(8).toString());
            Event spanning = new Event("Long trip", date.minusDays(2).toString(), date.plusDays(10).toString());
            TaskList tasks = new TaskList(List.of(today, outside, lastDay, tomorrow, spanning));
            Gui ui = new Gui();
            Clock clock = Clock.fixed(date.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

            new Parser(tasks, ui, clock).parse("upcoming");

            assertEquals("Upcoming (" + date.plusDays(1) + " to " + date.plusDays(7) + "):\n5. " + spanning
                    + "\n4. " + tomorrow + "\n3. " + lastDay, ui.getResponses());
        }
    }

    @Test
    public void completedTasksAndUndatedTodosAreExcluded() throws Exception {
        Deadline overdue = new Deadline("Old report", "2026-10-30");
        Deadline due = new Deadline("Report", "2026-11-01");
        Event event = new Event("Trip", "2026-11-01", "2026-11-05");
        for (Task task : List.of(overdue, due, event)) {
            task.markAsDone();
        }
        TaskList tasks = new TaskList(List.of(new Todo("Read"), overdue, due, event));

        assertEquals(List.of(), tasks.overdueDeadlines(TODAY));
        assertEquals(List.of(), tasks.tasksBetween(TODAY, TODAY.plusDays(7)));
        event.markAsNotDone();
        assertEquals(List.of(event), tasks.tasksBetween(TODAY, TODAY.plusDays(7)));
    }

    @Test
    public void emptyViewsExplainTheirDateRange() throws Exception {
        Gui ui = new Gui();
        Parser parser = new Parser(new TaskList(List.of()), ui, CLOCK);
        parser.parse("\u2003today\u2003");
        assertEquals("Today (2026-11-01):\nOverdue deadlines:\nNo overdue deadlines.\n"
                + "Due or scheduled today:\nNo incomplete deadlines or events today.", ui.getResponses());
        ui.clearResponses();
        parser.parse(" upcoming ");
        assertEquals("Upcoming (2026-11-02 to 2026-11-08):\n"
                + "No incomplete deadlines or events in the next 7 days.", ui.getResponses());
    }

    @Test
    public void invalidViewArgumentsAndCaseLeaveTasksUnchanged() throws Exception {
        Deadline task = new Deadline("Report", "2026-11-01");
        TaskList tasks = new TaskList(List.of(task));
        Parser parser = new Parser(tasks, new Gui(), CLOCK);
        for (String command : List.of("today 1", "upcoming 14", "today\nlist", "TODAY", "Upcoming")) {
            assertThrows(ArnException.class, () -> parser.parse(command), command);
            assertEquals(List.of(task), tasks.getTasks());
            assertFalse(task.isDone());
        }
    }

    @Test
    public void dateViewsUseTheClockZoneAndRefreshAfterMidnight() throws Exception {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-01T15:59:00Z"));
        Clock clock = new ViewClock(now, ZoneId.of("Asia/Singapore"));
        Deadline task = new Deadline("Tomorrow", "2026-11-02");
        Gui ui = new Gui();
        Parser parser = new Parser(new TaskList(List.of(task)), ui, clock);
        parser.parse("upcoming");
        assertTrue(ui.getResponses().contains("1. " + task));
        now.set(Instant.parse("2026-11-01T16:00:00Z"));
        ui.clearResponses();
        parser.parse("today");
        String afterMidnight = ui.getResponses();
        assertTrue(afterMidnight.startsWith("Today (2026-11-02):"));
        assertTrue(afterMidnight.contains("1. " + task));
        ui.clearResponses();
        parser.parse("upcoming");
        assertFalse(ui.getResponses().contains("1. " + task));
    }

    @Test
    public void viewListsAreIndependentAndInvalidRangesAreRejected() throws Exception {
        Deadline task = new Deadline("Report", "2026-11-01");
        TaskList tasks = new TaskList(List.of(task));
        tasks.tasksBetween(TODAY, TODAY).clear();
        tasks.overdueDeadlines(TODAY.plusDays(1)).clear();
        assertEquals(List.of(task), tasks.getTasks());
        assertThrows(IllegalArgumentException.class, () -> tasks.tasksBetween(TODAY, TODAY.minusDays(1)));
    }

    private static final class ViewClock extends Clock {
        private final AtomicReference<Instant> now;
        private final ZoneId zone;

        ViewClock(AtomicReference<Instant> now, ZoneId zone) {
            this.now = now;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new ViewClock(now, newZone);
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
