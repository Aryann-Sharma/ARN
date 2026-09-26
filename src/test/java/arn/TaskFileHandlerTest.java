package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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
        ArrayList<Task> tasks = handler.readTasks();

        assertEquals(1, tasks.size());
        assertEquals("valid task", tasks.get(0).description);
    }

    @Test
    public void delimitersInDescriptionsSurvivePersistence(@TempDir Path tempDir) throws ArnException {
        Path saveFile = tempDir.resolve("arn.txt");
        TaskFileHandler handler = new TaskFileHandler(saveFile.toString());
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(new Todo("buy milk | eggs"));
        tasks.add(new Deadline("submit | report", "2026-10-03 2359"));
        tasks.add(new Event("lunch | planning", "2026-10-04 1200", "2026-10-04 1330"));

        handler.writeTasks(tasks);
        ArrayList<Task> restored = handler.readTasks();

        assertEquals(tasks.size(), restored.size());
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).toString(), restored.get(i).toString());
        }
        assertTrue(restored.stream().allMatch(task -> task.description.contains("|")));
    }
}
