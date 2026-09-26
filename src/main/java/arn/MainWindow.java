package arn;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class MainWindow extends AnchorPane {
    @FXML
    ScrollPane scrollPane;
    @FXML
    VBox dialogContainer;
    @FXML
    TextField userInput;
    @FXML
    Button sendButton;
    @FXML
    Label taskCountLabel;

    private Arn arn;

    private final Image arnImage = new Image(this.getClass().getResourceAsStream("/images/ArnLogo.png"));
    private final Image userImage = new Image(this.getClass().getResourceAsStream("/images/ArnUser.png"));

    @FXML
    public void initialize() {
        userInput.setAccessibleText("Command");
        userInput.setAccessibleHelp("Type a command and press Enter to send it. Use Examples for command syntax.");
    }

    public void setArn(Arn a) {
        arn = a;
        updateTaskCount();
        addDialogs(DialogBox.getArnDialog(
                "Hi, I'm Arn. I can help you capture todos, track deadlines, and plan events. "
                        + "Try a quick action above or type a command below.", arnImage));
        Platform.runLater(userInput::requestFocus);
    }

    @FXML
    private void handleUserInput() {
        submitCommand(userInput.getText(), true);
    }

    @FXML
    private void handleListTasks() {
        submitCommand("list", false);
    }

    @FXML
    private void handleSortByDate() {
        submitCommand("sort", false);
    }

    @FXML
    private void handleShowExamples() {
        addDialogs(DialogBox.getArnDialog(
                "Here are a few things you can ask me:\n\n"
                        + "todo Read a chapter\n"
                        + "deadline Submit report /by 2026-10-02 1800\n"
                        + "event Team lunch /from 2026-10-04 1200 /to 2026-10-04 1330\n"
                        + "mark 1  •  unmark 1  •  delete 1\n"
                        + "find report  •  list  •  sort", arnImage));
        userInput.requestFocus();
    }

    private void submitCommand(String rawInput, boolean clearInputOnSuccess) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            userInput.requestFocus();
            return;
        }

        String response = arn.getResponse(input);
        boolean isError = response.startsWith("Error:");
        addDialogs(
                DialogBox.getUserDialog(input, userImage),
                isError
                        ? DialogBox.getErrorDialog(response, arnImage)
                        : DialogBox.getArnDialog(response, arnImage)
        );
        if (clearInputOnSuccess && !isError) {
            userInput.clear();
        }
        updateTaskCount();
        userInput.requestFocus();
    }

    private void addDialogs(DialogBox... dialogs) {
        dialogContainer.getChildren().addAll(dialogs);
        // Scroll only for new messages so resizing does not interrupt reading older ones.
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void updateTaskCount() {
        int taskCount = arn.getTaskCount();
        taskCountLabel.setText(taskCount + (taskCount == 1 ? " task" : " tasks") + " saved locally");
    }
}
