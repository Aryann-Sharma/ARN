package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the distributable in a separate JVM with no Gradle runtime classpath. */
public class PackagedApplicationTest {
    @Test
    public void packagedDesktopSavesTasksAndClosesAfterFarewell(@TempDir Path directory) throws Exception {
        Path probeClasses = Path.of(PackagedGuiProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path output = directory.resolve("desktop.log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-cp", System.getProperty("arn.jar") + File.pathSeparator + probeClasses,
                "arn.PackagedGuiProbe", System.getProperty("arn.uiReport"))
                .directory(directory.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Packaged desktop did not finish its startup checks");
            assertEquals(0, process.exitValue(), Files.readString(output));
            assertTrue(Files.exists(Path.of(System.getProperty("arn.uiReport"), "desktop.png")));
            assertTrue(Files.exists(Path.of(System.getProperty("arn.uiReport"), "desktop-small.png")));
            assertTrue(Files.exists(Path.of(System.getProperty("arn.uiReport"), "desktop-farewell.png")));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    public void jarContainsEntryPointResourcesAndNativeLibraries() throws IOException {
        try (JarFile jar = new JarFile(System.getProperty("arn.jar"))) {
            assertEquals("arn.Launcher", jar.getManifest().getMainAttributes().getValue("Main-Class"));
            assertEquals(System.getProperty("arn.version"),
                    jar.getManifest().getMainAttributes().getValue("Implementation-Version"));
            for (String resource : List.of("view/MainWindow.fxml", "view/DialogBox.fxml", "styles/main.css",
                    "images/ArnLogo.png", "images/ArnUser.png", "glass.dll", "libglass.so", "libglass.dylib")) {
                assertTrue(jar.getEntry(resource) != null, "Missing packaged resource: " + resource);
            }
        }
    }

    @Test
    public void packagedConsolePersistsAndReloadsTasks(@TempDir Path directory) throws Exception {
        Result first = run(directory, "todo Read café 中文 🌍 notes\ndeadline Report /by 2026-10-02 1800\nmark 2\n  bye  \n", "--cli");
        assertEquals(0, first.exitCode(), first.output());
        assertTrue(first.output().contains("Bye. Hope to see you again soon!"));
        assertFalse(first.output().contains("Exception"), first.output());
        assertTrue(Files.readString(directory.resolve("data/arn.txt")).contains("Read café 中文 🌍 notes"));

        Result second = run(directory, "list\nfind report\nsort\n", "--cli");
        assertEquals(0, second.exitCode(), second.output());
        assertTrue(second.output().contains("1. [T][ ] Read café 中文 🌍 notes"), second.output());
        assertTrue(second.output().contains("2. [D][X] Report"), second.output());
        assertFalse(second.output().contains("Exception"), second.output());
    }

    @Test
    public void packagedEditingAndUndoPersistAcrossLaunches(@TempDir Path directory) throws Exception {
        Result first = run(directory, "deadline Report /by 2026-10-02\nmark 1\nedit 1 Revised café report\n"
                + "reschedule 1 /by 2026-10-05 0000\nundo\nlist\nbye\n", "--cli");
        assertEquals(0, first.exitCode(), first.output());
        assertFalse(first.output().contains("Error:"), first.output());
        assertTrue(first.output().contains("1. [D][X] Revised café report (by Oct 2 2026)"), first.output());

        Result second = run(directory, "undo\nlist\nbye\n", "--cli");
        assertEquals(0, second.exitCode(), second.output());
        assertTrue(second.output().contains("Error: Nothing to undo."), second.output());
        assertTrue(second.output().contains("1. [D][X] Revised café report (by Oct 2 2026)"), second.output());
    }

    @Test
    public void packagedPartialRescheduleRetainsTimesAcrossRestarts(@TempDir Path directory) throws Exception {
        Result first = run(directory, "event Trip /from 2026-10-02 0000 /to 2026-10-04 1600\nmark 1\n"
                + "reschedule 1 /from 2026-10-03\nreschedule 1 /to 2026-10-05\nbye\n", "--cli");
        assertEquals(0, first.exitCode(), first.output());
        assertFalse(first.output().contains("Error:"), first.output());
        assertTrue(first.output().contains("from Oct 3 2026, 12:00AM to Oct 5 2026, 4:00PM"), first.output());

        Result second = run(directory, "list\nreschedule 1 /to 2026-10-06\nundo\nlist\nbye\n", "--cli");
        assertEquals(0, second.exitCode(), second.output());
        assertFalse(second.output().contains("Error:"), second.output());
        assertTrue(second.output().contains("1. [E][X] Trip (from Oct 3 2026, 12:00AM to Oct 5 2026, 4:00PM)"),
                second.output());
        assertTrue(Files.readString(directory.resolve("data/arn.txt"))
                .contains("E | 1 | Trip | 2026-10-03 0000 | 2026-10-05 1600"));
    }

    @Test
    public void malformedSaveStopsConsoleWithoutOverwritingData(@TempDir Path directory) throws Exception {
        Path saveFile = directory.resolve("data/arn.txt");
        Files.createDirectories(saveFile.getParent());
        String data = "# Arn data v1\nT | 0 | saved task\ninvalid record\n";
        Files.writeString(saveFile, data);

        Result result = run(directory, "", "--cli");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("line 3"), result.output());
        assertFalse(result.output().contains("StorageException"), result.output());
        assertEquals(data, Files.readString(saveFile));
    }

    @Test
    public void consoleRejectsMalformedByeAndStopsAfterSuccessfulBye(@TempDir Path directory) throws Exception {
        Result result = run(directory, "bye now\ntodo Before exit\nbye\ntodo After exit\n", "--cli");
        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("Error:"), result.output());
        assertTrue(result.output().contains("Bye. Hope to see you again soon!"));
        assertFalse(result.output().contains("Closing in 3 seconds."));
        String saved = Files.readString(directory.resolve("data/arn.txt"));
        assertTrue(saved.contains("Before exit"));
        assertFalse(saved.contains("After exit"));
    }

    @Test
    public void packagedHelpVersionAndInvalidOptions(@TempDir Path directory) throws Exception {
        Result help = run(directory, "", "--help");
        assertEquals(0, help.exitCode());
        assertTrue(help.output().contains("--cli"));
        Result version = run(directory, "", "--version");
        assertEquals(0, version.exitCode());
        assertEquals("Arn " + System.getProperty("arn.version"), version.output().strip());
        Result unknown = run(directory, "", "--unknown");
        assertEquals(2, unknown.exitCode());
        assertTrue(unknown.output().contains("Unknown launch option \"--unknown\""), unknown.output());
        assertTrue(unknown.output().contains("--help"), unknown.output());
        assertTrue(unknown.output().contains("Usage: java -jar Arn.jar"), unknown.output());
        Result multiple = run(directory, "", "--cli", "--version");
        assertEquals(2, multiple.exitCode());
        assertTrue(multiple.output().contains("Expected at most one launch option"), multiple.output());
        assertTrue(multiple.output().contains("on its own"), multiple.output());
        assertFalse(Files.exists(directory.resolve("data")));
    }

    private Result run(Path directory, String input, String... arguments) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "-Dfile.encoding=windows-1252", "-jar", System.getProperty("arn.jar")));
        command.addAll(List.of(arguments));
        Path output = Files.createTempFile(directory, "process-", ".log");
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            try (var stdin = process.getOutputStream()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Packaged application did not exit");
            return new Result(process.exitValue(), Files.readString(output));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private record Result(int exitCode, String output) {
    }
}
