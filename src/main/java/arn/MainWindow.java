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
        dialogContainer.heightProperty().addListener((observable, oldHeight, newHeight) ->
                scrollPane.setVvalue(1.0));
    }

    public void setArn(Arn a) {
        arn = a;
        updateTaskCount();
        dialogContainer.getChildren().add(DialogBox.getArnDialog(
                "Hi, I'm Arn. I can help you capture todos, track deadlines, and plan events. "
                        + "Try a quick action above or type a command below.", arnImage));
        Platform.runLater(userInput::requestFocus);
    }

    @FXML
    private void handleUserInput() {
        submitCommand(userInput.getText());
    }

    @FXML
    private void handleListTasks() {
        submitCommand("list");
    }

    @FXML
    private void handleSortByDate() {
        submitCommand("sort");
    }

    @FXML
    private void handleShowExamples() {
        dialogContainer.getChildren().add(DialogBox.getArnDialog(
                "Here are a few things you can ask me:\n\n"
                        + "todo Read a chapter\n"
                        + "deadline Submit report /by 2026-10-02 1800\n"
                        + "event Team lunch /from 2026-10-04 1200 /to 2026-10-04 1330\n"
                        + "mark 1  •  unmark 1  •  delete 1\n"
                        + "find report  •  list  •  sort", arnImage));
        userInput.requestFocus();
    }

    private void submitCommand(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.isEmpty()) {
            taskCountLabel.setText("Type a command to get started");
            userInput.requestFocus();
            return;
        }

        String response = arn.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                response.startsWith("Error:")
                        ? DialogBox.getErrorDialog(response, arnImage)
                        : DialogBox.getArnDialog(response, arnImage)
        );
        userInput.clear();
        updateTaskCount();
        userInput.requestFocus();
    }

    private void updateTaskCount() {
        int taskCount = arn.getTaskCount();
        taskCountLabel.setText(taskCount + (taskCount == 1 ? " task" : " tasks") + " saved locally");
    }
}
