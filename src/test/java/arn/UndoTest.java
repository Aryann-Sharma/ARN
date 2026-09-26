package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class UndoTest {
    @Test
    public void everyMutationCanBeUndoneInOrderAndSaved(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        List<String> states = new ArrayList<>();
        List<String> commands = List.of("todo Read", "deadline Report /by 2026-10-02 0000",
                "event Meeting /from 2026-10-03 /to 2026-10-04", "mark 2", "edit 2 Revised report",
                "reschedule 2 /by 2026-10-05", "mark 3", "edit 3 Planning meeting",
                "reschedule 3 /from 2026-10-06 1200 /to 2026-10-06 1300", "unmark 2", "delete 1");
        for (String command : commands) {
            states.add(arn.getResponse("list"));
            assertFalse(arn.getResponse(command).startsWith("Error:"), command);
            assertEquals(arn.getResponse("list"), open(file).getResponse("list"));
        }

        for (int i = states.size() - 1; i >= 0; i--) {
            assertTrue(arn.getResponse("undo").startsWith("Undid"));
            assertEquals(states.get(i), arn.getResponse("list"));
            assertEquals(states.get(i), open(file).getResponse("list"));
        }
        assertTrue(arn.getResponse("undo").startsWith("Error: Nothing to undo."));
    }

    @Test
    public void queriesErrorsAndUnchangedCommandsDoNotConsumeUndo(@TempDir Path directory) throws Exception {
        Arn arn = open(directory.resolve("arn.txt"));
        arn.getResponse("deadline Report /by 2026-10-02");
        for (String command : List.of("list", "sort", "find Report", "unmark 1", "edit 1 Report",
                "reschedule 1 /by 2026-10-02", "edit 1", "reschedule 1 /by invalid", "undo extra")) {
            arn.getResponse(command);
        }

        assertTrue(arn.getResponse("undo").startsWith("Undid"));
        assertEquals(0, arn.getTaskCount());
        assertTrue(arn.getResponse("undo").startsWith("Error:"));
    }

    @Test
    public void failedChangesAndFailedUndoCanBeRetried(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        arn.getResponse("deadline Report /by 2026-10-02");
        arn.getResponse("mark 1");
        String before = arn.getResponse("list");
        String saved = Files.readString(file);

        try (FileChannel channel = FileChannel.open(directory.resolve("arn.txt.lock"), StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            for (String command : List.of("edit 1 Revised report", "reschedule 1 /by 2026-10-05", "undo")) {
                assertTrue(arn.getResponse(command).startsWith("Error:"), command);
                assertEquals(before, arn.getResponse("list"));
                assertEquals(saved, Files.readString(file));
            }
        }

        assertTrue(arn.getResponse("undo").startsWith("Undid"));
        assertFalse(arn.taskList.get(0).isDone());
        assertEquals("Report", arn.taskList.get(0).getDescription());
        assertTrue(arn.getResponse("undo").startsWith("Undid"));
        assertEquals(0, arn.getTaskCount());
    }

    @Test
    public void failedUndoRestoresReplacedAndDeletedTasks(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        arn.getResponse("todo Original");
        arn.getResponse("mark 1");
        for (String command : List.of("edit 1 Revised", "delete 1")) {
            arn.getResponse(command);
            String before = arn.getResponse("list");
            try (FileChannel channel = FileChannel.open(directory.resolve("arn.txt.lock"), StandardOpenOption.WRITE);
                    FileLock lock = channel.lock()) {
                assertTrue(lock.isValid());
                assertTrue(arn.getResponse("undo").startsWith("Error:"));
                assertEquals(before, arn.getResponse("list"));
            }
            assertTrue(arn.getResponse("undo").startsWith("Undid"));
            assertEquals("1. [T][X] Original", arn.getResponse("list"));
        }
    }

    @Test
    public void undoRefusesToOverwriteExternalChanges(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        arn.getResponse("todo First");
        Arn other = open(file);
        other.getResponse("todo Second");
        String saved = Files.readString(file);

        assertTrue(arn.getResponse("undo").contains("changed outside this session"));
        assertEquals(1, arn.getTaskCount());
        assertEquals(saved, Files.readString(file));
    }

    @Test
    public void undoHistoryIsSessionOnlyAndNewChangesCanBeUndone(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("arn.txt");
        Arn arn = open(file);
        assertTrue(arn.getResponse("undo").startsWith("Error:"));
        assertFalse(Files.exists(file));
        arn.getResponse("todo Original");
        arn.getResponse("edit 1 Revised");
        arn.getResponse("undo");
        arn.getResponse("edit 1 Different revision");
        arn.getResponse("undo");
        assertEquals("1. [T][ ] Original", arn.getResponse("list"));

        arn.initialize(new TaskFileHandler(file));
        assertTrue(arn.getResponse("undo").startsWith("Error:"));
        assertEquals("1. [T][ ] Original", arn.getResponse("list"));
    }

    @Test
    public void keepsOnlyTheLastHundredChanges(@TempDir Path directory) throws Exception {
        Arn arn = open(directory.resolve("arn.txt"));
        arn.getResponse("todo First");
        for (int i = 1; i <= 100; i++) {
            assertFalse(arn.getResponse("edit 1 Revision " + i).startsWith("Error:"));
        }
        for (int i = 0; i < 100; i++) {
            assertTrue(arn.getResponse("undo").startsWith("Undid"));
        }
        assertEquals("1. [T][ ] First", arn.getResponse("list"));
        assertTrue(arn.getResponse("undo").startsWith("Error:"));
    }

    private Arn open(Path file) throws StorageException {
        Arn arn = new Arn();
        arn.initialize(new TaskFileHandler(file));
        return arn;
    }
}
