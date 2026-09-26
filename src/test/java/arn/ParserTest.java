package arn;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;

//Used ChatGPT to generate JUnit test cases
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
}

