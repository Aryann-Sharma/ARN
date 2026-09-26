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

    @Test
    public void testEmptyListErrorsExplainThatATaskMustBeAdded() {
        TaskList taskList = new TaskList(List.of());

        ArnException getError = assertThrows(ArnException.class, () -> taskList.get(0));
        ArnException removeError = assertThrows(ArnException.class, () -> taskList.remove(0));

        assertEquals("Your task list is empty. Add a task first, then use list to see its number.",
                getError.getMessage());
        assertEquals(getError.getMessage(), removeError.getMessage());
    }

    @Test
    public void testSingleTaskErrorsIdentifyTheOnlyAvailableNumber() {
        TaskList taskList = new TaskList(List.of(new Todo("read")));

        ArnException getError = assertThrows(ArnException.class, () -> taskList.get(1));
        ArnException removeError = assertThrows(ArnException.class, () -> taskList.remove(1));

        assertEquals("Task number 2 does not exist. Your only task is number 1. Use list to see it.",
                getError.getMessage());
        assertEquals(getError.getMessage(), removeError.getMessage());
    }

    @Test
    public void testOutOfRangeErrorsShowTheCurrentOneBasedRange() throws ArnException {
        TaskList taskList = new TaskList(List.of(new Todo("read"), new Todo("write"), new Todo("exercise")));

        ArnException getError = assertThrows(ArnException.class, () -> taskList.get(3));
        ArnException removeError = assertThrows(ArnException.class, () -> taskList.remove(3));
        assertEquals("Task number 4 does not exist. Choose a number from 1 to 3. Use list to see your tasks.",
                getError.getMessage());
        assertEquals(getError.getMessage(), removeError.getMessage());

        taskList.remove(0);
        ArnException afterRemoval = assertThrows(ArnException.class, () -> taskList.get(2));
        assertEquals("Task number 3 does not exist. Choose a number from 1 to 2. Use list to see your tasks.",
                afterRemoval.getMessage());
    }

    @Test
    public void testLargeIndexIsReportedWithoutIntegerOverflow() {
        TaskList taskList = new TaskList(List.of(new Todo("read"), new Todo("write")));

        ArnException error = assertThrows(ArnException.class, () -> taskList.get(Integer.MAX_VALUE));

        assertEquals("Task number 2147483648 does not exist. Choose a number from 1 to 2. "
                + "Use list to see your tasks.", error.getMessage());
    }
}
