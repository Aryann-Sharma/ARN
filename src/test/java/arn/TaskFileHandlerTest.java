package arn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        List<String[]> invalidRecords = List.of(
                new String[]{"broken", "Expected TYPE | STATUS | DESCRIPTION"},
                new String[]{"Z | 0 | unknown", "Unknown task type \"Z\""},
                new String[]{"T | invalid | bad status", "Use 0 for incomplete or 1 for complete"},
                new String[]{"D | 0 | missing date", "missing its due date field"},
                new String[]{"E | 0 | missing end | 2026-10-04", "missing a start or end date field"},
                new String[]{"D | 0 | invalid date | 2026-02-30", "2026-02-30"},
                new String[]{"E | 0 | bad start | 2026-02-30 | 2026-03-01", "Event start date"},
                new String[]{"E | 0 | bad end | 2026-02-01 | 2026-02-30", "Event end date"},
                new String[]{"E | 0 | bad time | 2026-10-04 1200 | 2026-10-04 2400", "Event end time"},
                new String[]{"E | 0 | backward event | 2026-10-04 | 2026-10-03", "before"},
                new String[]{"T | 0 |    ", "description"});
        for (String[] example : invalidRecords) {
            String invalidRecord = example[0];
            String contents = TaskFileHandler.DATA_HEADER + "\nT | 0 | valid task\n" + invalidRecord;
            Files.writeString(saveFile, contents);
            TaskFileHandler handler = new TaskFileHandler(saveFile);

            StorageException error = assertThrows(StorageException.class, handler::readTasks, invalidRecord);
            assertTrue(error.getMessage().contains("line 3"), error.getMessage());
            assertTrue(error.getMessage().contains(example[1]), error.getMessage());
            assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
            assertTrue(error.getMessage().contains("Back up the file"), error.getMessage());
            assertTrue(error.getMessage().contains("restart Arn"), error.getMessage());
            assertNotNull(error.getCause());
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
        List<String[]> invalidFiles = List.of(
                new String[]{"# Arn data v2\nT | 0 | future format", "Unsupported", "compatible backup"},
                new String[]{TaskFileHandler.DATA_HEADER + "\n" + TaskFileHandler.DATA_HEADER,
                        "Duplicate", "before any tasks"},
                new String[]{"T | 0 | legacy task\n" + TaskFileHandler.DATA_HEADER,
                        "Misplaced", "before any tasks"});
        for (String[] example : invalidFiles) {
            String contents = example[0];
            Files.writeString(saveFile, contents);
            TaskFileHandler handler = new TaskFileHandler(saveFile);

            StorageException error = assertThrows(StorageException.class, handler::readTasks);

            assertTrue(error.getMessage().contains("header"), error.getMessage());
            assertTrue(error.getMessage().contains(example[1]), error.getMessage());
            assertTrue(error.getMessage().contains(example[2]), error.getMessage());
            assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
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
        assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
        assertTrue(error.getMessage().contains("Restore a valid UTF-8 backup"), error.getMessage());
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

        StorageException unsupportedType = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("valid task"), new Task("unsupported task"))));
        assertTrue(unsupportedType.getMessage().contains("task type \"Task\""), unsupportedType.getMessage());
        assertTrue(unsupportedType.getMessage().contains(saveFile.toString()), unsupportedType.getMessage());
        assertEquals(originalData, Files.readString(saveFile));
        StorageException invalidUnicode = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("invalid Unicode: \uD800"))));
        assertTrue(invalidUnicode.getMessage().contains("invalid Unicode"), invalidUnicode.getMessage());
        assertTrue(invalidUnicode.getMessage().contains("Retype the description"), invalidUnicode.getMessage());
        assertTrue(invalidUnicode.getMessage().contains(saveFile.toString()), invalidUnicode.getMessage());
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

        StorageException error = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("replacement"))));
        assertTrue(error.getMessage().contains("has not been loaded successfully"), error.getMessage());
        assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
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
        assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
        assertTrue(error.getMessage().contains("Restart Arn to load the latest saved tasks"), error.getMessage());
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

        StorageException changed = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("stale task"))));
        assertTrue(changed.getMessage().contains("changed outside this session"), changed.getMessage());
        assertEquals("T | 0 | externally changed task", Files.readString(saveFile));
        handler.readTasks();
        Files.delete(saveFile);
        StorageException removed = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("stale task"))));
        assertTrue(removed.getMessage().contains("removed or moved"), removed.getMessage());
        assertTrue(removed.getMessage().contains("Restore the file"), removed.getMessage());
        assertTrue(removed.getMessage().contains(saveFile.toString()), removed.getMessage());
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
            assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
            assertTrue(error.getMessage().contains("Wait for it to finish and retry"), error.getMessage());
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

        StorageException readError = assertThrows(StorageException.class, handler::readTasks);
        StorageException writeError = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("task"))));
        for (StorageException error : List.of(readError, writeError)) {
            assertTrue(error.getMessage().contains(tempDir.toString()), error.getMessage());
            assertTrue(error.getMessage().contains("this path is a directory"), error.getMessage());
            assertTrue(error.getMessage().contains("Move or rename that directory"), error.getMessage());
            assertFalse(error.getMessage().contains("permission"), error.getMessage());
        }
    }

    @Test
    public void fileBlockingParentDirectoryIsIdentified(@TempDir Path tempDir) throws Exception {
        Path blockingFile = tempDir.resolve("data");
        Files.writeString(blockingFile, "keep this file");
        Path saveFile = blockingFile.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile);

        StorageException readError = assertThrows(StorageException.class, handler::readTasks);
        StorageException writeError = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("task"))));

        for (StorageException error : List.of(readError, writeError)) {
            assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
            assertTrue(error.getMessage().contains("parent path \"" + blockingFile + "\""), error.getMessage());
            assertTrue(error.getMessage().contains("not a directory"), error.getMessage());
            assertTrue(error.getMessage().contains("Move or rename the blocking file"), error.getMessage());
        }
        assertEquals("keep this file", Files.readString(blockingFile));
    }

    @Test
    public void directoryBlockingLockFileHasSpecificRecoveryAdvice(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        Path lockPath = tempDir.resolve("arn.txt.lock");
        Files.createDirectory(lockPath);
        TaskFileHandler handler = new TaskFileHandler(saveFile);

        StorageException error = assertThrows(StorageException.class,
                () -> handler.writeTasks(List.of(new Todo("task"))));

        assertTrue(error.getMessage().contains(saveFile.toString()), error.getMessage());
        assertTrue(error.getMessage().contains("lock path \"" + lockPath + "\" is a directory"), error.getMessage());
        assertTrue(error.getMessage().contains("Move or rename that directory"), error.getMessage());
        assertFalse(Files.exists(saveFile));
        assertTrue(Files.isDirectory(lockPath));
    }
}
