package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class TaskListTest {
    @Test
    public void testAddAndRemoveTask() throws ArnException {
        TaskList taskList = new TaskList(new ArrayList<>());

        Todo todo = new Todo("gym");
        taskList.add(todo);

        assertEquals(1, taskList.size());
        assertEquals(todo, taskList.get(0));

        Task removed = taskList.remove(0);
        assertEquals(todo, removed);
        assertEquals(0, taskList.size());
    }

    @Test
    public void testRemoveFromEmptyList() {
        TaskList taskList = new TaskList(new ArrayList<>());

        assertThrows(ArnException.class, () -> taskList.remove(0));
    }

    @Test
    public void testInvalidIndicesDoNotChangeTheList() {
        Todo todo = new Todo("gym");
        TaskList taskList = new TaskList(List.of(todo));

        for (int index : new int[]{-1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(ArnException.class, () -> taskList.get(index));
            assertThrows(ArnException.class, () -> taskList.remove(index));
        }
        assertEquals(List.of(todo), taskList.getTasks());
    }

    @Test
    public void testCollectionBoundariesAreDefensive() {
        Todo todo = new Todo("gym");
        List<Task> source = new ArrayList<>(List.of(todo));
        TaskList taskList = new TaskList(source);
        List<Task> snapshot = taskList.getTasks();

        source.clear();
        assertEquals(List.of(todo), taskList.getTasks());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        taskList.add(new Todo("read"));
        assertEquals(List.of(todo), snapshot);
    }

    @Test
    public void testNullTasksAreRejectedImmediately() {
        assertThrows(NullPointerException.class, () -> new TaskList(null));
        assertThrows(NullPointerException.class, () -> new TaskList(Arrays.asList(new Todo("gym"), null)));
        TaskList taskList = new TaskList(List.of());

        assertThrows(NullPointerException.class, () -> taskList.add(null));
        assertEquals(0, taskList.size());
    }

    @Test
    public void testSearchIsCaseInsensitiveRegardlessOfSystemLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Todo first = new Todo("WRITE report");
            Todo second = new Todo("write notes");
            TaskList taskList = new TaskList(List.of(first, new Todo("read book"), second));

            assertEquals(List.of(first, second), taskList.find("write"));
            assertEquals(List.of(first, second), taskList.find("WRITE"));
            assertEquals(List.of(), taskList.find("missing"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void testDateSortIsStableAndDoesNotChangeSavedOrder() throws ArnException {
        Todo todo = new Todo("gym");
        Deadline later = new Deadline("report", "2026-10-02");
        Event first = new Event("meeting", "2026-10-01 0900", "2026-10-01 1000");
        Deadline second = new Deadline("notes", "2026-10-01 0900");
        List<Task> savedOrder = List.of(todo, later, first, second);
        TaskList taskList = new TaskList(savedOrder);

        List<Task> sorted = taskList.sortByDate();
        assertEquals(List.of(first, second, later), sorted);
        assertEquals(savedOrder, taskList.getTasks());
        sorted.clear();
        assertEquals(savedOrder, taskList.getTasks());
    }
}

