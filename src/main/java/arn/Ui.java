package arn;

import java.util.Scanner;

public class Ui {
    private final Scanner scanner;

    public Ui() {
        scanner = new Scanner(System.in);
    }

    public String readCommand() {
        System.out.print("-> ");
        return scanner.nextLine();
    }

    public void displayGreet() {
        System.out.println("Hello! I'm Arn");
        System.out.println("What can I do for you?");
        System.out.print("\n");
    }

    public void displayBye() {
        System.out.println("Bye. Hope to see you again soon!");
    }

    public void displayMsg(String msg) {
        System.out.println(msg);
    }

    public void close() {
        scanner.close();
    }
}
