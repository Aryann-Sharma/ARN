package arn;

import java.util.ArrayList;
import java.util.List;

public class Gui extends Ui {
    private final List<String> responses;
    private boolean exitRequested;

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
        super.displayBye();
        exitRequested = true;
    }

    boolean isExitRequested() {
        return exitRequested;
    }

    public String getResponses() {
        String result = String.join("\n", responses);
        clearResponses();
        return result;
    }

    public void clearResponses() {
        responses.clear();
        exitRequested = false;
    }
}
