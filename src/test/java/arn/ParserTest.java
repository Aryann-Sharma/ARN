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

        assertEquals("Add a description after 'todo'. Example: todo Read a chapter", todoError.getMessage());
        assertEquals("Add a task number after 'mark'. Example: mark 1. Use 'list' to see task numbers.",
                markError.getMessage());
    }

    @Test
    public void testEventClausesInWrongOrderReturnValidationError() {
        TaskList taskList = new TaskList(new ArrayList<>());
        Parser parser = new Parser(taskList, new Gui());

        ArnException error = assertThrows(ArnException.class,
                () -> parser.parse("event review /to 2026-10-02 /from 2026-10-01"));

        assertTrue(error.getMessage().startsWith("Put '/from' before '/to'"), error.getMessage());
        assertTrue(error.getMessage().contains("Example: event "), error.getMessage());
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
        parser.parse("event explain /to syntax /from 2026-10-01 /to 2026-10-02");

        assertEquals("review /bylaws and path/by", taskList.get(0).getDescription());
        assertEquals("review /fromage and /today", taskList.get(1).getDescription());
        assertEquals("explain /to syntax", taskList.get(2).getDescription());
    }

    @Test
    public void testClausesMustBeSeparateTokens() {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());

        String[][] commands = {
            {"deadline submit report/by", "/by"},
            {"deadline report/by 2026-10-01", "/by"},
            {"deadline report /by2026-10-01", "/by"},
            {"event trip/from 2026-10-01 /to 2026-10-02", "/from"},
            {"event trip /from2026-10-01 /to 2026-10-02", "/from"},
            {"event trip /from 2026-10-01 /to2026-10-02", "/to"}
        };
        for (String[] command : commands) {
            ArnException error = assertThrows(ArnException.class, () -> parser.parse(command[0]), command[0]);
            assertTrue(error.getMessage().contains("'" + command[1] + "' as a separate marker"),
                    error.getMessage());
            assertTrue(error.getMessage().contains("space before it and before the date"), error.getMessage());
            assertTrue(error.getMessage().contains("Example: "), error.getMessage());
        }
        assertEquals(0, taskList.size());
    }

    @Test
    public void testMissingFieldsIdentifyWhatToAdd() {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());
        String[][] commands = {
            {"deadline /by 2026-10-02", "description before '/by'"},
            {"deadline report /by", "due date after '/by'"},
            {"event /from 2026-10-01 /to 2026-10-02", "description before '/from'"},
            {"event review /from /to 2026-10-02", "start date after '/from' and before '/to'"},
            {"event review /from 2026-10-01 /to", "end date after '/to'"},
            {"find", "text to search for after 'find'"}
        };
        for (String[] command : commands) {
            ArnException error = assertThrows(ArnException.class, () -> parser.parse(command[0]), command[0]);
            assertTrue(error.getMessage().contains(command[1]), error.getMessage());
        }
        assertEquals(0, taskList.size());
    }

    @Test
    public void testRepeatedDateMarkersHaveSpecificErrors() {
        TaskList taskList = new TaskList(List.of());
        Parser parser = new Parser(taskList, new Gui());
        String[][] commands = {
            {"deadline report /by 2026-10-01 /by 2026-10-02", "/by"},
            {"event review /from 2026-10-01 /from 2026-10-02 /to 2026-10-03", "/from"},
            {"event review /from 2026-10-01 /to 2026-10-02 /to 2026-10-03", "/to"}
        };
        for (String[] command : commands) {
            ArnException error = assertThrows(ArnException.class, () -> parser.parse(command[0]), command[0]);
            assertTrue(error.getMessage().startsWith("Use '" + command[1] + "' only once."), error.getMessage());
        }
        assertEquals(0, taskList.size());
    }

    @Test
    public void testCommandSpellingAndExtraArgumentsHaveDifferentErrors() {
        Parser parser = new Parser(new TaskList(List.of()), new Gui());
        ArnException uppercase = assertThrows(ArnException.class, () -> parser.parse("TODO Read a chapter"));
        assertEquals("Command names are case-sensitive. Use 'todo' instead of 'TODO'.", uppercase.getMessage());
        ArnException unknown = assertThrows(ArnException.class, () -> parser.parse("todoctor visit"));
        assertTrue(unknown.getMessage().startsWith("Unknown command 'todoctor'. Available commands:"));

        for (String command : List.of("list", "sort", "bye")) {
            ArnException extra = assertThrows(ArnException.class, () -> parser.parse(command + " now"));
            assertEquals("'" + command + "' does not take extra text. Enter just '" + command + "'.",
                    extra.getMessage());
        }
    }

    @Test
    public void testTaskNumberSyntaxErrorsExplainTheCommand() {
        TaskList taskList = new TaskList(List.of(new Todo("existing task")));
        Parser parser = new Parser(taskList, new Gui());
        for (String command : List.of("mark", "unmark", "delete")) {
            ArnException missing = assertThrows(ArnException.class, () -> parser.parse(command));
            assertTrue(missing.getMessage().contains("Add a task number after '" + command + "'"));
            for (String number : List.of("0", "-1", "1.5", "abc", "1 2")) {
                ArnException invalid = assertThrows(ArnException.class, () -> parser.parse(command + " " + number));
                assertTrue(invalid.getMessage().contains("one whole task number after '" + command + "'"));
                assertTrue(invalid.getMessage().contains("starting at 1"));
            }
        }
        assertEquals(1, taskList.size());
        assertFalse(assertDoesNotThrow(() -> taskList.get(0)).isDone());
    }

    @Test
    public void testRecoveryExamplesAreValidCommands() {
        for (String input : List.of("todo", "deadline report/by", "deadline /by 2026-10-02",
                "deadline report /by", "event meeting", "event meeting /from /to 2026-10-02",
                "event meeting /to 2026-10-02 /from 2026-10-01", "find")) {
            TaskList taskList = new TaskList(List.of());
            Parser parser = new Parser(taskList, new Gui());
            ArnException error = assertThrows(ArnException.class, () -> parser.parse(input), input);
            assertEquals(0, taskList.size(), input);
            String marker = "Example: ";
            assertTrue(error.getMessage().contains(marker), error.getMessage());
            String example = error.getMessage().substring(error.getMessage().indexOf(marker) + marker.length());
            assertDoesNotThrow(() -> parser.parse(example), example);
        }
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
