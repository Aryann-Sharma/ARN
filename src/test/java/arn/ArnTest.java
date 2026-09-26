package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ArnTest {
    @Test
    public void commandsPersistAcrossRestarts(@TempDir Path directory) throws Exception {
        Path saveFile = directory.resolve("arn.txt");
        Arn arn = open(saveFile);

        assertFalse(arn.getResponse("todo Read café notes | chapter 2").startsWith("Error:"));
        assertFalse(arn.getResponse("deadline Submit report /by 2026-10-02 1800").startsWith("Error:"));
        assertFalse(arn.getResponse("event Review /from 2026-10-03 /to 2026-10-04").startsWith("Error:"));
        arn.getResponse("mark 2");

        Arn reopened = open(saveFile);
        assertEquals(3, reopened.getTaskCount());
        assertEquals(arn.getResponse("list"), reopened.getResponse("list"));
        assertTrue(reopened.taskList.get(1).isDone());
        reopened.getResponse("unmark 2");
        reopened.getResponse("delete 1");

        Arn reopenedAgain = open(saveFile);
        assertEquals(2, reopenedAgain.getTaskCount());
        assertFalse(reopenedAgain.taskList.get(0).isDone());
        assertEquals("Submit report", reopenedAgain.taskList.get(0).getDescription());
    }

    @Test
    public void everyFailedMutationRestoresPreviousState(@TempDir Path directory) throws Exception {
        List<String> commands = List.of("todo New task", "deadline Due /by 2026-10-05",
                "event Meeting /from 2026-10-05 /to 2026-10-06", "delete 1", "mark 1", "unmark 2");
        for (int i = 0; i < commands.size(); i++) {
            Path saveFile = directory.resolve("case-" + i).resolve("arn.txt");
            Arn arn = open(saveFile);
            arn.getResponse("todo First task");
            arn.getResponse("todo Second task");
            arn.getResponse("mark 2");
            String before = arn.getResponse("list");

            Files.delete(saveFile);
            Files.createDirectory(saveFile);
            String response = arn.getResponse(commands.get(i));

            assertTrue(response.startsWith("Error:"), commands.get(i));
            assertFalse(response.contains("added:"), "A failed save must not report success");
            assertEquals(before, arn.getResponse("list"), commands.get(i));
            assertEquals(2, arn.getTaskCount());
        }
    }

    @Test
    public void readOnlyAndUnchangedCommandsDoNotWrite(@TempDir Path directory) throws Exception {
        Path saveFile = directory.resolve("arn.txt");
        Arn arn = open(saveFile);
        arn.getResponse("todo Read a book");
        arn.getResponse("deadline Report /by 2026-10-03");
        arn.getResponse("mark 1");
        Files.delete(saveFile);
        Files.createDirectory(saveFile);

        for (String command : List.of("list", "find book", "sort", "bye", "mark 1", "unmark 2",
                "edit 1 Read a book", "reschedule 2 /by 2026-10-03")) {
            assertFalse(arn.getResponse(command).startsWith("Error:"), command);
        }
        assertTrue(arn.getResponse("todo Needs saving").startsWith("Error:"));
        assertEquals(2, arn.getTaskCount());
    }

    @Test
    public void invalidCommandsLeaveSavedDataAndNextResponseUntouched(@TempDir Path directory) throws Exception {
        Path saveFile = directory.resolve("arn.txt");
        Arn arn = open(saveFile);
        arn.getResponse("todo Original task");
        String saved = Files.readString(saveFile);

        assertTrue(arn.getResponse("mark 999").startsWith("Error:"));
        assertEquals(saved, Files.readString(saveFile));
        assertEquals("1. [T][ ] Original task", arn.getResponse("list"));
    }

    @Test
    public void onlySuccessfulByeRequestsExit(@TempDir Path directory) throws Exception {
        Path saveFile = directory.resolve("arn.txt");
        Arn arn = open(saveFile);
        assertFalse(arn.isExitRequested());
        assertTrue(arn.getResponse("bye now").startsWith("Error:"));
        assertFalse(arn.isExitRequested());
        assertFalse(arn.getResponse("todo Say bye to a friend").startsWith("Error:"));
        assertFalse(arn.isExitRequested());
        String saved = Files.readString(saveFile);

        assertEquals("Bye. Hope to see you again soon!", arn.getResponse("  bye  "));
        assertTrue(arn.isExitRequested());
        assertEquals(saved, Files.readString(saveFile));
        assertTrue(arn.getResponse("bye now").startsWith("Error:"));
        assertFalse(arn.isExitRequested(), "A rejected command must not reuse an earlier exit signal");
    }

    private Arn open(Path file) throws StorageException {
        Arn arn = new Arn();
        arn.initialize(new TaskFileHandler(file));
        return arn;
    }
}
