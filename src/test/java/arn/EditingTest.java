package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class EditingTest {
    @Test
    public void editingEachTypePreservesDatesStatusAndPosition() throws Exception {
        TaskList tasks = new TaskList(List.of(new Todo("Read"), new Deadline("Report", "2026-10-02 0000"),
                new Event("Meeting", "2026-10-03", "2026-10-04")));
        tasks.get(1).markAsDone();
        Parser parser = new Parser(tasks, new Gui());

        parser.parse("edit 1 Read chapter two");
        parser.parse("edit 2 Revised report");
        parser.parse("edit 3 Planning meeting");

        assertEquals("[T][ ] Read chapter two", tasks.get(0).toString());
        assertEquals("Revised report", tasks.get(1).getDescription());
        assertTrue(tasks.get(1).isDone());
        assertEquals("2026-10-02 0000", ((Deadline) tasks.get(1)).formatDate(false));
        assertEquals("Planning meeting", tasks.get(2).getDescription());
        assertEquals("2026-10-03", ((Event) tasks.get(2)).formatStartDate(false));
        assertEquals("2026-10-04", ((Event) tasks.get(2)).formatEndDate(false));
        assertFalse(tasks.get(2).isDone());
        assertEquals(3, tasks.size());
    }

    @Test
    public void editTreatsMarkersAsDescriptionTextAndAcceptsUnicodeWhitespace() throws Exception {
        TaskList tasks = new TaskList(List.of(new Todo("Original")));
        Parser parser = new Parser(tasks, new Gui());

        parser.parse("\u2003edit\u20031\u2003Review café | 中文 /by tomorrow\u2003");

        assertEquals("Review café | 中文 /by tomorrow", tasks.get(0).getDescription());
    }

    @Test
    public void reschedulingPreservesDescriptionStatusAndTaskNumbers() throws Exception {
        TaskList tasks = new TaskList(List.of(new Todo("First"), new Deadline("Report", "2026-10-02"),
                new Event("Meeting", "2026-10-03 1200", "2026-10-03 1300")));
        tasks.get(1).markAsDone();
        tasks.get(2).markAsDone();
        Parser parser = new Parser(tasks, new Gui());

        parser.parse("reschedule 2 /by 2026-10-05 0000");
        parser.parse("reschedule 3 /from 2026-10-06 /to 2026-10-07");

        assertEquals("First", tasks.get(0).getDescription());
        assertEquals("Report", tasks.get(1).getDescription());
        assertTrue(tasks.get(1).isDone());
        assertEquals("2026-10-05 0000", ((Deadline) tasks.get(1)).formatDate(false));
        assertEquals("Meeting", tasks.get(2).getDescription());
        assertTrue(tasks.get(2).isDone());
        assertEquals("2026-10-06", ((Event) tasks.get(2)).formatStartDate(false));
        assertEquals("2026-10-07", ((Event) tasks.get(2)).formatEndDate(false));
        assertEquals(3, tasks.size());
    }

    @Test
    public void malformedEditsAndReschedulesDoNotReplaceTasks() throws Exception {
        TaskList tasks = new TaskList(List.of(new Todo("Read"), new Deadline("Report", "2026-10-02"),
                new Event("Meeting", "2026-10-03", "2026-10-04")));
        List<Task> original = tasks.getTasks();
        Parser parser = new Parser(tasks, new Gui());
        for (String command : List.of("edit", "edit 1", "edit 1   ", "edit 0 Test", "edit -1 Test",
                "edit 4 Test", "edit abc Test", "edit 999999999999 Test", "edit 1 First\nSecond",
                "reschedule", "reschedule 2", "reschedule abc /by 2026-10-02",
                "reschedule 1 /by 2026-10-02", "reschedule 2 /by", "reschedule 2 /by2026-10-02",
                "reschedule 2 extra /by 2026-10-02", "reschedule 2 /by 2026-02-30",
                "reschedule 2 /by 2026-10-02 2400", "reschedule 2 /by 2026-10-02 /by 2026-10-03",
                "reschedule 2 /from 2026-10-02 /to 2026-10-03", "reschedule 3 /by 2026-10-02",
                "reschedule 3 /from 2026-10-03", "reschedule 3 /from /to 2026-10-04",
                "reschedule 3 /from 2026-10-03 /to", "reschedule 3 /from 2026-10-03 /to 2026-10-02",
                "reschedule 3 /from 2026-10-03 /to 2026-10-04 1200",
                "reschedule 3 /to 2026-10-04 /from 2026-10-03",
                "reschedule 3 /from 2026-10-03 /from 2026-10-04 /to 2026-10-05",
                "reschedule 3 /from 2026-10-03 /to 2026-10-04 /to 2026-10-05", "undo now")) {
            assertThrows(ArnException.class, () -> parser.parse(command), command);
            assertEquals(original, tasks.getTasks(), command);
        }
    }

    @Test
    public void identicalEditsKeepExistingObjectsButExplicitMidnightIsAChange() throws Exception {
        TaskList tasks = new TaskList(List.of(new Todo("Read"), new Deadline("Report", "2026-10-02"),
                new Event("Meeting", "2026-10-03", "2026-10-04")));
        List<Task> original = tasks.getTasks();
        Parser parser = new Parser(tasks, new Gui());

        parser.parse("edit 1 Read");
        parser.parse("reschedule 2 /by 2026-10-02");
        parser.parse("reschedule 3 /from 2026-10-03 /to 2026-10-04");
        assertEquals(original, tasks.getTasks());
        parser.parse("reschedule 2 /by 2026-10-02 0000");
        assertEquals("2026-10-02 0000", ((Deadline) tasks.get(1)).formatDate(false));
    }
}
