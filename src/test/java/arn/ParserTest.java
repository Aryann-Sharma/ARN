package arn;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void testTodoCommand() throws ArnException {
        TaskList taskList = new TaskList(new ArrayList<>());
        Ui ui = new Ui();
        Parser parser = new Parser(taskList, ui);

        parser.parse("todo buy milk");
        assertEquals(1, taskList.size());
        assertTrue(taskList.get(0).toString().contains("buy milk"));
    }

    @Test
    public void testInvalidCommand() {
        TaskList taskList = new TaskList(new ArrayList<>());
        Ui ui = new Ui();
        Parser parser = new Parser(taskList, ui);

        assertThrows(ArnException.class, () -> parser.parse("invalid command"));
    }

    @Test
    public void testListCommandWhenEmpty() throws ArnException {
        TaskList taskList = new TaskList(new ArrayList<>());
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        parser.parse("list");

        assertEquals("Your task list is empty. Add a todo, deadline, or event to get started.",
                gui.getResponses());
    }

    @Test
    public void testDeleteCommand() throws ArnException {
        TaskList taskList = new TaskList(new ArrayList<>());
        taskList.add(new Todo("gym"));
        Ui ui = new Ui();
        Parser parser = new Parser(taskList, ui);

        parser.parse("delete 1");
        assertEquals(0, taskList.size());
    }

    @Test
    public void testTaskNumberValidation() {
        TaskList taskList = new TaskList(new ArrayList<>());
        taskList.add(new Todo("seed task"));
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        assertThrows(ArnException.class, () -> parser.parse("mark abc"));
        assertThrows(ArnException.class, () -> parser.parse("unmark abc"));
        assertThrows(ArnException.class, () -> parser.parse("delete abc"));
        assertThrows(ArnException.class,
                () -> parser.parse("mark 999999999999999999999999999"));
        assertDoesNotThrow(() -> parser.parse("mark  1"));
    }

    @Test
    public void testMissingCommandArgumentsHaveSpecificErrors() {
        TaskList taskList = new TaskList(new ArrayList<>());
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        ArnException todoError = assertThrows(ArnException.class, () -> parser.parse("todo"));
        ArnException markError = assertThrows(ArnException.class, () -> parser.parse("mark"));

        assertEquals("Empty task description.", todoError.getMessage());
        assertEquals("No task number provided to mark.", markError.getMessage());
    }

    @Test
    public void testEventClausesInWrongOrderReturnValidationError() {
        TaskList taskList = new TaskList(new ArrayList<>());
        Parser parser = new Parser(taskList, new Gui());

        ArnException error = assertThrows(ArnException.class,
                () -> parser.parse("event review /to 2026-10-02 /from 2026-10-01"));

        assertEquals("Event task must have both '/from' and '/to' clauses.", error.getMessage());
        assertEquals(0, taskList.size());
    }

    @Test
    public void testMalformedCommandsReturnExpectedValidationErrors() {
        TaskList taskList = new TaskList(new ArrayList<>());
        Parser parser = new Parser(taskList, new Gui());
        List<String> malformedCommands = List.of(
                "",
                "deadline report",
                "deadline /by 2026-10-02",
                "deadline report /by",
                "event review",
                "event review /from /to 2026-10-02",
                "event review /from 2026-10-01 /to",
                "find",
                "delete",
                "unmark",
                "mark 0",
                "delete -1"
        );

        for (String command : malformedCommands) {
            assertThrows(ArnException.class, () -> parser.parse(command), command);
        }
        assertEquals(0, taskList.size());
    }

    @Test
    public void testTaskNumbersInSearchReferToSavedOrder() throws ArnException {
        TaskList taskList = new TaskList(List.of(new Todo("read book"), new Todo("buy milk")));
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        parser.parse("find milk");

        assertEquals("Here are the matching tasks in your list:\n2. [T][ ] buy milk", gui.getResponses());
        parser.parse("mark 2");
        assertFalse(taskList.get(0).isDone());
        assertTrue(taskList.get(1).isDone());
    }

    @Test
    public void testTaskNumbersInSortedViewReferToSavedOrder() throws ArnException {
        Todo todo = new Todo("read book");
        Deadline later = new Deadline("submit report", "2026-10-03");
        Event earlier = new Event("meeting", "2026-10-01", "2026-10-01");
        TaskList taskList = new TaskList(List.of(todo, later, earlier));
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        parser.parse("sort");

        assertEquals("3. " + earlier + "\n2. " + later, gui.getResponses());
        assertEquals(List.of(todo, later, earlier), taskList.getTasks());
        parser.parse("delete 3");
        assertEquals(List.of(todo, later), taskList.getTasks());
    }

    @Test
    public void testClauseNamesWithinDescriptionArePreserved() throws ArnException {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());

        parser.parse("deadline review /bylaws and path/by /by 2026-10-01");
        parser.parse("event review /fromage and /today /from 2026-10-01 /to 2026-10-02");

        assertEquals("review /bylaws and path/by", taskList.get(0).getDescription());
        assertEquals("review /fromage and /today", taskList.get(1).getDescription());
    }

    @Test
    public void testClausesMustBeSeparateTokens() {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());

        assertThrows(ArnException.class, () -> parser.parse("deadline report/by 2026-10-01"));
        assertThrows(ArnException.class, () -> parser.parse("deadline report /by2026-10-01"));
        assertThrows(ArnException.class,
                () -> parser.parse("event trip /from2026-10-01 /to 2026-10-02"));
        assertThrows(ArnException.class,
                () -> parser.parse("event trip /from 2026-10-01 /to2026-10-02"));
        assertEquals(0, taskList.size());
    }

    @Test
    public void testTabsAndSurroundingWhitespaceAreAccepted() throws ArnException {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());

        parser.parse(" \ttodo\tread book\t ");
        parser.parse("deadline\tsubmit report\t/by\t2026-10-01");
        parser.parse("event\tmeeting\t/from\t2026-10-01\t/to\t2026-10-02");
        parser.parse("mark\t1");

        assertEquals(3, taskList.size());
        assertEquals("read book", taskList.get(0).getDescription());
        assertTrue(taskList.get(0).isDone());
    }

    @Test
    public void testUnicodeWhitespaceIsHandledConsistently() throws ArnException {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());

        parser.parse("\u2003deadline\u2003report\u2003/by\u20032026-10-01\u2003");
        parser.parse("mark\u20031");

        assertEquals("report", taskList.get(0).getDescription());
        assertTrue(taskList.get(0).isDone());
    }

    @Test
    public void testMultilineCommandsAreRejectedWithoutMutation() {
        Todo task = new Todo("existing task");
        TaskList taskList = new TaskList(List.of(task));
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        for (String command : List.of("todo first\nT | 0 | second", "todo first\rsecond", "mark\n1")) {
            ArnException error = assertThrows(ArnException.class, () -> parser.parse(command));
            assertEquals("Enter one command on a single line.", error.getMessage());
        }

        assertEquals(List.of(task), taskList.getTasks());
        assertFalse(task.isDone());
        assertEquals("", gui.getResponses());
    }

    @Test
    public void testInvalidDatesDoNotAddTasks() {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());

        assertThrows(ArnException.class, () -> parser.parse("deadline report /by 2025-02-29"));
        assertThrows(ArnException.class,
                () -> parser.parse("event meeting /from 2026-10-01 2400 /to 2026-10-02 0100"));
        assertEquals(0, taskList.size());
    }

    @Test
    public void testCommandPrefixesAndExtraArgumentsAreRejected() {
        TaskList taskList = new TaskList(List.of(new Todo("existing task")));
        Parser parser = new Parser(taskList, new Gui());

        for (String command : List.of("todoctor visit", "marked 1", "list 1", "sort 1", "bye now",
                "mark 1 2", "delete 2", "unmark 0", "mark +1")) {
            assertThrows(ArnException.class, () -> parser.parse(command), command);
        }
        assertEquals(1, taskList.size());
    }

    @Test
    public void testMatchingTasksWithTheSameDescriptionKeepSeparateNumbers() throws ArnException {
        TaskList taskList = new TaskList(List.of(new Todo("read"), new Todo("same"), new Todo("same")));
        Gui gui = new Gui();
        Parser parser = new Parser(taskList, gui);

        parser.parse("find same");

        assertEquals("Here are the matching tasks in your list:\n2. [T][ ] same\n3. [T][ ] same",
                gui.getResponses());
        parser.parse("mark 3");
        assertFalse(taskList.get(1).isDone());
        assertTrue(taskList.get(2).isDone());
    }

    @Test
    public void testGeneratedInputsNeverCauseUncheckedErrorsOrPartialChanges() throws ArnException {
        Random random = new Random(74129);
        String[] prefixes = {"", "todo", "todo ", "deadline ", "event ", "mark ", "unmark ",
            "delete ", "find ", "list ", "sort ", "bye "};
        String alphabet = " abcdefghijklmnopqrstuvwxyz0123456789/by/from/to|-+\t\r\n\u2003";
        for (int i = 0; i < 2000; i++) {
            Todo original = new Todo("existing task");
            TaskList taskList = new TaskList(List.of(original));
            Parser parser = new Parser(taskList, new Gui());
            StringBuilder input = new StringBuilder(prefixes[random.nextInt(prefixes.length)]);
            int length = random.nextInt(60);
            for (int j = 0; j < length; j++) {
                input.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }

            try {
                parser.parse(input.toString());
            } catch (ArnException e) {
                assertEquals(List.of(original), taskList.getTasks(), input.toString());
                assertFalse(original.isDone(), input.toString());
            }
        }
    }
}
