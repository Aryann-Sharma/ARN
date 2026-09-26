package arn;

import java.util.ArrayList;
import java.util.List;

public class Gui extends Ui {
    private final List<String> responses;
    private boolean exitRequested;
    private boolean undoRequested;

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

    @Override
    public void requestUndo() {
        undoRequested = true;
    }

    boolean isUndoRequested() {
        return undoRequested;
    }

    public String getResponses() {
        String result = String.join("\n", responses);
        clearResponses();
        return result;
    }

    public void clearResponses() {
        responses.clear();
        exitRequested = false;
        undoRequested = false;
    }
}
