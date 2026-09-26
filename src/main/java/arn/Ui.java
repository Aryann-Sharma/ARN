package arn;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Ui implements AutoCloseable {
    private final InputStream input;
    private final PrintStream output;
    private Scanner scanner;

    public Ui() {
        this(System.in, new PrintStream(System.out, true, StandardCharsets.UTF_8));
    }

    Ui(InputStream input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public String readCommand() {
        if (scanner == null) {
            scanner = new Scanner(input, StandardCharsets.UTF_8);
        }
        output.print("-> ");
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    public void displayGreet() {
        displayMsg("Hello! I'm Arn");
        displayMsg("What can I do for you?");
        displayMsg("");
    }

    public void displayBye() {
        displayMsg("Bye. Hope to see you again soon!");
    }

    public void displayMsg(String msg) {
        output.println(msg);
    }

    public void requestUndo() throws ArnException {
        throw new ArnException("Undo must run through an active Arn session.");
    }

    @Override
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
