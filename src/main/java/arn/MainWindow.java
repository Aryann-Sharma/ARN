package arn;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

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
    @FXML
    Button listButton;
    @FXML
    Button sortButton;
    @FXML
    Button examplesButton;

    private Arn arn;
    private boolean exiting;

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
        if (exiting) {
            return;
        }
        addDialogs(DialogBox.getArnDialog(
                "Here are a few things you can ask me:\n\n"
                        + "todo Read a chapter\n"
                        + "deadline Submit report /by 2026-10-02 1800\n"
                        + "event Team lunch /from 2026-10-04 1200 /to 2026-10-04 1330\n"
                        + "mark 1  •  unmark 1  •  delete 1\n"
                        + "edit 1 Read chapter two\n"
                        + "reschedule 2 /by 2026-10-05 1800\n"
                        + "reschedule 3 /from 2026-10-05 1200 /to 2026-10-05 1330\n"
                        + "undo (last saved change in this session)\n"
                        + "find report  •  list  •  sort", arnImage));
        userInput.requestFocus();
    }

    private void submitCommand(String rawInput, boolean clearInputOnSuccess) {
        if (exiting) {
            return;
        }
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            userInput.requestFocus();
            return;
        }

        String response = arn.getResponse(input);
        boolean isError = response.startsWith("Error:");
        if (arn.isExitRequested()) {
            response += "\nClosing in 3 seconds.";
        }
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
        if (arn.isExitRequested()) {
            closeAfterFarewell();
        } else {
            userInput.requestFocus();
        }
    }

    private void closeAfterFarewell() {
        exiting = true;
        userInput.setDisable(true);
        sendButton.setDisable(true);
        listButton.setDisable(true);
        sortButton.setDisable(true);
        examplesButton.setDisable(true);

        Window window = userInput.getScene() == null ? null : userInput.getScene().getWindow();
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(event -> {
            if (window != null) {
                window.hide();
            }
        });
        pause.play();
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
