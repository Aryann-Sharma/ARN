package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class MainWindowTest {
    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        CountDownLatch toolkitStarted = new CountDownLatch(1);
        Platform.startup(toolkitStarted::countDown);
        assertTrue(toolkitStarted.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @AfterAll
    public static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    public void refreshedInterfaceHandlesCoreInteractions(@TempDir Path tempDir) throws Exception {
        runOnFxThread(() -> {
            Arn arn = createTestArn(tempDir);
            FXMLLoader loader = new FXMLLoader(Arn.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Arn.class.getResource("/styles/main.css").toExternalForm());
            root.applyCss();
            root.layout();

            MainWindow controller = loader.getController();
            controller.setArn(arn);

            assertNotNull(root.lookup(".app-header"));
            assertEquals(1, scene.getStylesheets().size());
            assertEquals(1, controller.dialogContainer.getChildren().size());
            assertEquals("0 tasks saved locally", controller.taskCountLabel.getText());

            controller.userInput.setText("todo Test the refreshed UI");
            controller.sendButton.fire();

            assertEquals(3, controller.dialogContainer.getChildren().size());
            assertEquals("1 task saved locally", controller.taskCountLabel.getText());
            DialogBox userMessage = (DialogBox) controller.dialogContainer.getChildren().get(1);
            assertTrue(userMessage.getStyleClass().contains("user-dialog"));
            assertTrue(userMessage.dialog.getStyleClass().contains("message-bubble"));

            controller.userInput.setText("   ");
            controller.sendButton.fire();
            assertEquals(3, controller.dialogContainer.getChildren().size());
            assertEquals("Type a command to get started", controller.taskCountLabel.getText());

            controller.userInput.setText("not-a-command");
            controller.sendButton.fire();
            assertEquals(5, controller.dialogContainer.getChildren().size());
            DialogBox errorMessage = (DialogBox) controller.dialogContainer.getChildren().get(4);
            assertTrue(errorMessage.getStyleClass().contains("error-dialog"));

            controller.userInput.setText("mark abc");
            controller.sendButton.fire();
            assertEquals(7, controller.dialogContainer.getChildren().size());
            DialogBox invalidNumberMessage = (DialogBox) controller.dialogContainer.getChildren().get(6);
            assertTrue(invalidNumberMessage.getStyleClass().contains("error-dialog"));

            controller.userInput.setText("mark  1");
            controller.sendButton.fire();
            assertEquals(9, controller.dialogContainer.getChildren().size());
            DialogBox repeatedSpaceMessage = (DialogBox) controller.dialogContainer.getChildren().get(8);
            assertTrue(repeatedSpaceMessage.getStyleClass().contains("arn-dialog"));
            return null;
        });
    }

    @Test
    public void saveFailureIsShownAndUnsavedChangeIsRolledBack(@TempDir Path tempDir) throws Exception {
        runOnFxThread(() -> {
            Arn arn = createTestArn(tempDir);
            arn.taskFileHandler = new TaskFileHandler(tempDir);

            FXMLLoader loader = new FXMLLoader(Arn.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = loader.load();
            MainWindow controller = loader.getController();
            controller.setArn(arn);

            controller.userInput.setText("todo should not be retained");
            controller.sendButton.fire();

            assertEquals(0, arn.getTaskCount());
            assertEquals("0 tasks saved locally", controller.taskCountLabel.getText());
            DialogBox errorMessage = (DialogBox) controller.dialogContainer.getChildren().get(2);
            assertTrue(errorMessage.getStyleClass().contains("error-dialog"));
            assertEquals("Error: Could not save tasks.", errorMessage.dialog.getText());
            return null;
        });
    }

    private Arn createTestArn(Path tempDir) {
        Arn arn = new Arn();
        arn.taskFileHandler = new TaskFileHandler(tempDir.resolve("arn.txt").toString());
        arn.taskList = new TaskList(new ArrayList<>());
        arn.gui = new Gui();
        arn.parser = new Parser(arn.taskList, arn.gui);
        return arn;
    }

    private <T> T runOnFxThread(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
