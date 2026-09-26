package arn;

import java.util.ArrayList;
import java.util.List;

public class Gui extends Ui {
    private final List<String> responses;

    public Gui() {
        super();
        responses = new ArrayList<>();
    }

    @Override
    public void displayMsg(String msg) {
        responses.add(msg);
    }

    @Override
    public void displayBye() {
        responses.add("Bye. Hope to see you again soon!");
    }

    public String getResponses() {
        String result = String.join("\n", responses);
        clearResponses();
        return result;
    }

    public void clearResponses() {
        responses.clear();
    }
}
