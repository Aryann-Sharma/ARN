package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ToDoTest {
    @Test
    public void testMarkAsDoneAndToString() {
        Todo todo = new Todo("read book");
        assertEquals("[T][ ] read book", todo.toString());

        todo.markAsDone();
        assertEquals("[T][X] read book", todo.toString());

        todo.markAsNotDone();
        assertEquals("[T][ ] read book", todo.toString());
    }

    @Test
    public void testDescriptionIsTrimmedWithoutChangingItsContent() {
        Todo todo = new Todo(" \t read  book | notes \u2003");

        assertEquals("read  book | notes", todo.getDescription());
    }

    @Test
    public void testBlankAndMultilineDescriptionsAreRejected() {
        assertThrows(NullPointerException.class, () -> new Todo(null));
        for (String description : new String[]{"", " \t", "\u2003", "read\nbook", "read\rbook"}) {
            assertThrows(IllegalArgumentException.class, () -> new Todo(description));
        }
    }
}
