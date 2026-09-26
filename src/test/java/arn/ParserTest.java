package arn;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
}

