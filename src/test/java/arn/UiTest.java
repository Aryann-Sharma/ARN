package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class UiTest {
    @Test
    public void consoleReadsUtf8AndEndsCleanlyAtEndOfInput() {
        ByteArrayInputStream input = new ByteArrayInputStream("todo café\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Ui ui = new Ui(input, new PrintStream(output, true, StandardCharsets.UTF_8))) {
            assertEquals("todo café", ui.readCommand());
            assertNull(ui.readCommand());
        }
    }
}
