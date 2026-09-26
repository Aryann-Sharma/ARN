package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TaskFileHandlerTest {
    @Test
    public void malformedLinesAreSkipped(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        Files.writeString(saveFile, String.join(System.lineSeparator(),
                "broken",
                "Z | 0 | unknown",
                "T | invalid | bad status",
                "D | 0 | missing date",
                "T | 0 | valid task"));

        TaskFileHandler handler = new TaskFileHandler(saveFile.toString());
        List<Task> tasks = handler.readTasks();

        assertEquals(1, tasks.size());
        assertEquals("valid task", tasks.get(0).getDescription());
    }

    @Test
    public void delimitersInDescriptionsSurvivePersistence(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile.toString());
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(new Todo("buy milk | eggs"));
        tasks.add(new Deadline("submit | report", "2026-10-03 2359"));
        tasks.add(new Event("lunch | planning", "2026-10-04 1200", "2026-10-04 1330"));

        handler.writeTasks(tasks);
        List<Task> restored = handler.readTasks();

        assertEquals(tasks.size(), restored.size());
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).toString(), restored.get(i).toString());
        }
        assertTrue(restored.stream().allMatch(task -> task.getDescription().contains("|")));
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
