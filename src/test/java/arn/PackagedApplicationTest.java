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
    public void packagedDesktopStartsAndSavesTasks(@TempDir Path directory) throws Exception {
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
    public void malformedSaveStopsConsoleWithoutOverwritingData(@TempDir Path directory) throws Exception {
        Path saveFile = directory.resolve("data/arn.txt");
        Files.createDirectories(saveFile.getParent());
        String data = "# Arn data v1\nT | 0 | saved task\ninvalid record\n";
        Files.writeString(saveFile, data);

        Result result = run(directory, "", "--cli");

        assertEquals(1, result.exitCode(), result.output());
        assertTrue(result.output().contains("line 3"), result.output());
        assertEquals(data, Files.readString(saveFile));
    }

    @Test
    public void packagedHelpVersionAndInvalidOptions(@TempDir Path directory) throws Exception {
        Result help = run(directory, "", "--help");
        assertEquals(0, help.exitCode());
        assertTrue(help.output().contains("--cli"));
        Result version = run(directory, "", "--version");
        assertEquals(0, version.exitCode());
        assertEquals("Arn " + System.getProperty("arn.version"), version.output().strip());
        assertEquals(2, run(directory, "", "--unknown").exitCode());
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
