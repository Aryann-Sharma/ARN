package arn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TaskFileHandlerTest {
    @Test
    public void malformedLinesAreReportedWithoutReturningPartialData(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        List<String> invalidRecords = List.of(
                "broken",
                "Z | 0 | unknown",
                "T | invalid | bad status",
                "D | 0 | missing date",
                "D | 0 | invalid date | 2026-02-30",
                "E | 0 | backward event | 2026-10-04 | 2026-10-03",
                "T | 0 |    ");
        for (String invalidRecord : invalidRecords) {
            String contents = TaskFileHandler.DATA_HEADER + "\nT | 0 | valid task\n" + invalidRecord;
            Files.writeString(saveFile, contents);
            TaskFileHandler handler = new TaskFileHandler(saveFile);

            StorageException error = assertThrows(StorageException.class, handler::readTasks, invalidRecord);
            assertTrue(error.getMessage().contains("line 3"), error.getMessage());
            assertThrows(StorageException.class, () -> handler.writeTasks(List.of(new Todo("replacement"))));
            assertEquals(contents, Files.readString(saveFile));
        }
    }

    @Test
    public void delimitersInDescriptionsSurvivePersistence(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile.toString());
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(new Todo("buy milk | eggs |"));
        tasks.add(new Deadline("submit | report", "2026-10-03 2359"));
        tasks.add(new Event("lunch | planning", "2026-10-04 1200", "2026-10-04 1330"));
        tasks.add(new Deadline("réviser | 中文", "2026-10-05"));
        tasks.add(new Event("day | trip 🌍", "2026-10-06", "2026-10-07"));
        tasks.get(0).markAsDone();
        tasks.get(2).markAsDone();
        tasks.get(3).markAsDone();

        handler.writeTasks(tasks);
        List<Task> restored = handler.readTasks();

        assertEquals(tasks.size(), restored.size());
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).toString(), restored.get(i).toString());
        }
        assertTrue(restored.stream().allMatch(task -> task.getDescription().contains("|")));
    }

    @Test
    public void unknownAndMisplacedHeadersAreRejected(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        List<String> invalidFiles = List.of(
                "# Arn data v2\nT | 0 | future format",
                TaskFileHandler.DATA_HEADER + "\n" + TaskFileHandler.DATA_HEADER,
                "T | 0 | legacy task\n" + TaskFileHandler.DATA_HEADER);
        for (String contents : invalidFiles) {
            Files.writeString(saveFile, contents);
            TaskFileHandler handler = new TaskFileHandler(saveFile);

            StorageException error = assertThrows(StorageException.class, handler::readTasks);

            assertTrue(error.getMessage().contains("header"), error.getMessage());
            assertEquals(contents, Files.readString(saveFile));
        }
    }

    @Test
    public void blankLinesAndUtf8ByteOrderMarkAreAccepted(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        Files.writeString(saveFile, "\uFEFF\r\n" + TaskFileHandler.DATA_HEADER
                + "\r\n\r\nT | 1 | read a book\r\n\r\n");

        List<Task> tasks = new TaskFileHandler(saveFile).readTasks();

        assertEquals(1, tasks.size());
        assertEquals("read a book", tasks.get(0).getDescription());
        assertTrue(tasks.get(0).isDone());
    }

    @Test
    public void invalidUtf8CannotBeLoadedOrOverwritten(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        byte[] invalidData = {(byte) 0xc3, (byte) 0x28};
        Files.write(saveFile, invalidData);
        TaskFileHandler handler = new TaskFileHandler(saveFile);

        StorageException error = assertThrows(StorageException.class, handler::readTasks);
        assertTrue(error.getMessage().contains("UTF-8"));
        assertThrows(StorageException.class, () -> handler.writeTasks(List.of(new Todo("replacement"))));
        assertArrayEquals(invalidData, Files.readAllBytes(saveFile));
    }

    @Test
    public void newSaveCreatesParentsOnlyWhenWriting(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("nested/data/arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);

        assertTrue(handler.readTasks().isEmpty());
        assertFalse(Files.exists(saveFile.getParent()));
        handler.writeTasks(List.of(new Todo("first task")));

        assertEquals("first task", new TaskFileHandler(saveFile).readTasks().get(0).getDescription());
    }

    @Test
    public void emptyTaskListRoundTrips(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);
        handler.writeTasks(List.of(new Todo("old task")));

        handler.writeTasks(List.of());

        assertTrue(handler.readTasks().isEmpty());
        assertEquals(TaskFileHandler.DATA_HEADER + "\n", Files.readString(saveFile));
    }

    @Test
    public void serializationFailureKeepsTheLastSave(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);
        handler.writeTasks(List.of(new Todo("original task")));
        String originalData = Files.readString(saveFile);

        assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("valid task"), new Task("unsupported task"))));
        assertEquals(originalData, Files.readString(saveFile));
        assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("invalid Unicode: \uD800"))));
        assertEquals(originalData, Files.readString(saveFile));
        try (var files = Files.list(tempDir)) {
            assertTrue(files.noneMatch(file -> file.getFileName().toString().endsWith(".tmp")));
        }
        handler.writeTasks(List.of(new Todo("later valid task")));
        assertEquals("later valid task", handler.readTasks().get(0).getDescription());
    }

    @Test
    public void unreadExistingSaveCannotBeOverwritten(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        Files.writeString(saveFile, "T | 0 | existing task");
        TaskFileHandler handler = new TaskFileHandler(saveFile);

        assertThrows(StorageException.class, () -> handler.writeTasks(List.of(new Todo("replacement"))));
        assertEquals("T | 0 | existing task", Files.readString(saveFile));
    }

    @Test
    public void olderInstanceCannotOverwriteNewerSave(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler first = new TaskFileHandler(saveFile);
        TaskFileHandler second = new TaskFileHandler(saveFile);
        first.readTasks();
        second.readTasks();
        first.writeTasks(List.of(new Todo("newer task")));

        StorageException error = assertThrows(StorageException.class,
                () -> second.writeTasks(List.of(new Todo("stale task"))));

        assertTrue(error.getMessage().contains("changed outside this session"));
        assertEquals("newer task", new TaskFileHandler(saveFile).readTasks().get(0).getDescription());
        second.readTasks();
        second.writeTasks(List.of(new Todo("updated after reloading")));
        assertEquals("updated after reloading", first.readTasks().get(0).getDescription());
    }

    @Test
    public void externalEditsAndDeletionAreDetected(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);
        handler.writeTasks(List.of(new Todo("original task")));
        Files.writeString(saveFile, "T | 0 | externally changed task");

        assertThrows(StorageException.class, () -> handler.writeTasks(List.of(new Todo("stale task"))));
        assertEquals("T | 0 | externally changed task", Files.readString(saveFile));
        handler.readTasks();
        Files.delete(saveFile);
        assertThrows(StorageException.class, () -> handler.writeTasks(List.of(new Todo("stale task"))));
        assertFalse(Files.exists(saveFile));
    }

    @Test
    public void heldLockPreventsSavingUntilReleased(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);
        handler.writeTasks(List.of(new Todo("original task")));
        String originalData = Files.readString(saveFile);
        try (FileChannel channel = FileChannel.open(tempDir.resolve("arn.txt.lock"), StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            assertTrue(lock.isValid());
            StorageException error = assertThrows(StorageException.class,
                    () -> handler.writeTasks(List.of(new Todo("replacement"))));
            assertTrue(error.getMessage().contains("Another instance"));
            assertEquals(originalData, Files.readString(saveFile));
        }

        handler.writeTasks(List.of(new Todo("replacement")));
        assertEquals("replacement", handler.readTasks().get(0).getDescription());
    }

    @Test
    public void concurrentFirstSessionsCannotLoseOneAnothersChanges(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("nested/arn.txt");
        CyclicBarrier bothLoaded = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(firstSession(saveFile, "first task", bothLoaded));
            Future<Boolean> second = executor.submit(firstSession(saveFile, "second task", bothLoaded));

            boolean firstSaved = first.get(10, TimeUnit.SECONDS);
            boolean secondSaved = second.get(10, TimeUnit.SECONDS);

            assertTrue(firstSaved ^ secondSaved, "Exactly one competing save should succeed");
            List<Task> tasks = new TaskFileHandler(saveFile).readTasks();
            assertEquals(1, tasks.size());
            assertEquals(firstSaved ? "first task" : "second task", tasks.get(0).getDescription());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private Callable<Boolean> firstSession(Path saveFile, String description, CyclicBarrier bothLoaded) {
        return () -> {
            TaskFileHandler handler = new TaskFileHandler(saveFile);
            assertTrue(handler.readTasks().isEmpty());
            bothLoaded.await(5, TimeUnit.SECONDS);
            try {
                handler.writeTasks(List.of(new Todo(description)));
                return true;
            } catch (StorageException e) {
                assertTrue(e.getMessage().contains("Another instance")
                        || e.getMessage().contains("changed outside this session"), e.getMessage());
                return false;
            }
        };
    }

    @Test
    public void writesVersionHeaderAndReadsLegacyFiles(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);

        handler.writeTasks(List.of(new Todo("versioned task")));
        assertEquals(TaskFileHandler.DATA_HEADER, Files.readAllLines(saveFile).get(0));

        Files.writeString(saveFile, "T | 0 | legacy task");
        List<Task> restored = handler.readTasks();
        assertEquals(1, restored.size());
        assertEquals("legacy task", restored.get(0).getDescription());
    }

    @Test
    public void inaccessibleSavePathProducesStorageError(@TempDir Path tempDir) {
        TaskFileHandler handler = new TaskFileHandler(tempDir);

        assertThrows(StorageException.class, handler::readTasks);
        assertThrows(StorageException.class, () -> handler.writeTasks(List.of(new Todo("task"))));
    }
}
