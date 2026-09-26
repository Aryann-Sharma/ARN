package arn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MainWindowTest {
    @BeforeAll
    public static void startJavaFx() throws InterruptedException {
        CountDownLatch toolkitStarted = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            toolkitStarted.countDown();
        });
        assertTrue(toolkitStarted.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @AfterAll
    public static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    public void mainWindowHandlesCoreInteractions(@TempDir Path tempDir) throws Exception {
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

            controller.userInput.setText("todo Read a chapter");
            controller.sendButton.fire();

            assertEquals(3, controller.dialogContainer.getChildren().size());
            assertEquals("1 task saved locally", controller.taskCountLabel.getText());
            assertEquals("", controller.userInput.getText());
            DialogBox userMessage = (DialogBox) controller.dialogContainer.getChildren().get(1);
            assertTrue(userMessage.getStyleClass().contains("user-dialog"));
            assertTrue(userMessage.dialog.getStyleClass().contains("message-bubble"));

            controller.userInput.setText("   ");
            controller.sendButton.fire();
            assertEquals(3, controller.dialogContainer.getChildren().size());
            assertEquals("1 task saved locally", controller.taskCountLabel.getText());

            controller.userInput.setText("not-a-command");
            controller.sendButton.fire();
            assertEquals(5, controller.dialogContainer.getChildren().size());
            DialogBox errorMessage = (DialogBox) controller.dialogContainer.getChildren().get(4);
            assertTrue(errorMessage.getStyleClass().contains("error-dialog"));
            assertEquals("not-a-command", controller.userInput.getText());

            controller.userInput.setText("mark abc");
            controller.sendButton.fire();
            assertEquals(7, controller.dialogContainer.getChildren().size());
            DialogBox invalidNumberMessage = (DialogBox) controller.dialogContainer.getChildren().get(6);
            assertTrue(invalidNumberMessage.getStyleClass().contains("error-dialog"));
            assertEquals("mark abc", controller.userInput.getText());

            controller.userInput.setText("mark  1");
            controller.sendButton.fire();
            assertEquals(9, controller.dialogContainer.getChildren().size());
            DialogBox repeatedSpaceMessage = (DialogBox) controller.dialogContainer.getChildren().get(8);
            assertTrue(repeatedSpaceMessage.getStyleClass().contains("arn-dialog"));
            return null;
        });
    }

    @Test
    public void editingReschedulingAndUndoWorkThroughTheDesktop(@TempDir Path tempDir) throws Exception {
        runOnFxThread(() -> {
            WindowFixture window = createWindow(tempDir);
            MainWindow controller = window.controller;
            for (String command : new String[]{"deadline Report /by 2026-10-02", "edit 1 Revised report",
                    "reschedule 1 /by 2026-10-05 1800", "undo"}) {
                controller.userInput.setText(command);
                controller.sendButton.fire();
                assertEquals("", controller.userInput.getText());
                assertEquals("1 task saved locally", controller.taskCountLabel.getText());
                DialogBox response = (DialogBox) controller.dialogContainer.getChildren()
                        .get(controller.dialogContainer.getChildren().size() - 1);
                assertTrue(response.getStyleClass().contains("arn-dialog"));
            }
            Arn reopened = new Arn();
            reopened.initialize(new TaskFileHandler(tempDir.resolve("arn.txt")));
            assertEquals("1. [D][ ] Revised report (by Oct 2 2026)", reopened.getResponse("list"));

            controller.userInput.setText("reschedule 1 /by invalid");
            controller.sendButton.fire();
            assertEquals("reschedule 1 /by invalid", controller.userInput.getText());
            controller.userInput.setText("undo");
            controller.sendButton.fire();
            controller.userInput.setText("undo");
            controller.sendButton.fire();
            assertEquals("0 tasks saved locally", controller.taskCountLabel.getText());
            controller.userInput.setText("undo");
            controller.sendButton.fire();
            assertEquals("undo", controller.userInput.getText());
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
            assertTrue(errorMessage.dialog.getText().startsWith("Error: Could not save tasks to "));
            assertTrue(errorMessage.dialog.getText().contains(tempDir.toString()));
            assertTrue(errorMessage.dialog.getText().contains("directory"));
            assertEquals("todo should not be retained", controller.userInput.getText());
            return null;
        });
    }

    @Test
    public void quickActionsPreserveUnfinishedCommand(@TempDir Path tempDir) throws Exception {
        runOnFxThread(() -> {
            WindowFixture window = createWindow(tempDir);
            MainWindow controller = window.controller;
            String draft = "deadline Submit report /by ";
            controller.userInput.setText(draft);

            ((Button) window.root.lookup("#listButton")).fire();
            assertEquals(draft, controller.userInput.getText());
            assertEquals(3, controller.dialogContainer.getChildren().size());

            ((Button) window.root.lookup("#sortButton")).fire();
            assertEquals(draft, controller.userInput.getText());
            assertEquals(5, controller.dialogContainer.getChildren().size());

            ((Button) window.root.lookup("#examplesButton")).fire();
            assertEquals(draft, controller.userInput.getText());
            assertEquals(6, controller.dialogContainer.getChildren().size());
            assertEquals("0 tasks saved locally", controller.taskCountLabel.getText());
            return null;
        });
    }

    @Test
    public void enterSubmitsAndMalformedCommandCanBeCorrected(@TempDir Path tempDir) throws Exception {
        runOnFxThread(() -> {
            WindowFixture window = createWindow(tempDir);
            MainWindow controller = window.controller;
            controller.userInput.setText("deadline Submit report /by invalid");
            controller.userInput.fireEvent(new javafx.event.ActionEvent());
            assertEquals("deadline Submit report /by invalid", controller.userInput.getText());
            assertEquals("0 tasks saved locally", controller.taskCountLabel.getText());

            controller.userInput.setText("deadline Submit report /by 2026-10-02");
            controller.userInput.fireEvent(new javafx.event.ActionEvent());
            assertEquals("", controller.userInput.getText());
            assertEquals("1 task saved locally", controller.taskCountLabel.getText());
            assertEquals(5, controller.dialogContainer.getChildren().size());
            return null;
        });
    }

    @Test
    public void resizingKeepsReadingPositionAndNewMessagesScrollToBottom(@TempDir Path tempDir) throws Exception {
        WindowFixture window = runOnFxThread(() -> {
            WindowFixture result = createWindow(tempDir);
            Button examples = (Button) result.root.lookup("#examplesButton");
            for (int i = 0; i < 20; i++) {
                examples.fire();
            }
            result.root.applyCss();
            result.root.layout();
            return result;
        });
        // A separate call lets queued scrolling from the new messages finish first.
        runOnFxThread(() -> {
            MainWindow controller = window.controller;
            assertEquals(1.0, controller.scrollPane.getVvalue());
            assertTrue(controller.dialogContainer.getHeight() > controller.scrollPane.getViewportBounds().getHeight());
            controller.scrollPane.setVvalue(0.25);
            window.root.resize(560, 640);
            window.root.layout();
            assertTrue(controller.scrollPane.getVvalue() < 0.5,
                    "Resizing should keep the conversation near the reading position: "
                            + controller.scrollPane.getVvalue());
            ((Button) window.root.lookup("#examplesButton")).fire();
            return null;
        });
        runOnFxThread(() -> {
            assertEquals(1.0, window.controller.scrollPane.getVvalue());
            return null;
        });
    }

    @Test
    public void wrappedErrorsStayVisibleAfterTheConversationGrows(@TempDir Path tempDir) throws Exception {
        for (int width : new int[]{700, 560}) {
            WindowFixture window = runOnFxThread(() -> {
                WindowFixture result = createWindow(tempDir.resolve("width-" + width));
                result.root.resize(width, 640);
                for (int i = 0; i < 8; i++) {
                    result.controller.examplesButton.fire();
                }
                result.root.applyCss();
                result.root.layout();
                return result;
            });
            for (double startingPosition : new double[]{1.0, 0.25, 1.0}) {
                runOnFxThread(() -> {
                    window.controller.scrollPane.setVvalue(startingPosition);
                    window.root.layout();
                    window.controller.userInput.setText("event Meeting /from 2026-10-01 /to 2026-10-02 1200");
                    window.controller.sendButton.fire();
                    return null;
                });
                runOnFxThread(() -> {
                    window.root.applyCss();
                    window.root.layout();
                    MainWindow controller = window.controller;
                    DialogBox response = (DialogBox) controller.dialogContainer.getChildren()
                            .get(controller.dialogContainer.getChildren().size() - 1);
                    javafx.scene.Node viewport = controller.scrollPane.lookup(".viewport");
                    javafx.geometry.Bounds visible = viewport.localToScene(viewport.getLayoutBounds());
                    javafx.geometry.Bounds message = response.localToScene(response.getLayoutBounds());
                    assertTrue(response.getStyleClass().contains("error-dialog"));
                    assertTrue(message.getMinY() >= visible.getMinY() - 1,
                            "The start of the error should be visible");
                    assertTrue(message.getMaxY() <= visible.getMaxY() + 1,
                            "The end of the error should be visible after layout at width " + width
                                    + ": message bottom=" + message.getMaxY() + ", viewport bottom="
                                    + visible.getMaxY());
                    return null;
                });
            }
        }
    }

    @Test
    public void longMessagesWrapWithinNarrowWindow(@TempDir Path tempDir) throws Exception {
        runOnFxThread(() -> {
            WindowFixture window = createWindow(tempDir);
            MainWindow controller = window.controller;
            String description = "chapter".repeat(150);
            controller.userInput.setText("todo " + description);
            controller.sendButton.fire();
            window.root.resize(520, 560);
            window.root.applyCss();
            window.root.layout();

            DialogBox userMessage = (DialogBox) controller.dialogContainer.getChildren().get(1);
            assertEquals("todo " + description, userMessage.dialog.getText());
            assertTrue(userMessage.dialog.isWrapText());
            assertEquals(userMessage.dialog.getText(), ((Text) userMessage.dialog.lookup(".text")).getText(),
                    "The full message should remain readable");
            assertTrue(userMessage.dialog.getHeight() >= userMessage.dialog.prefHeight(userMessage.dialog.getWidth()),
                    "The message needs enough height for every wrapped line");
            assertTrue(userMessage.dialog.getBoundsInParent().getMaxX() <= userMessage.getWidth(),
                    "The message must stay inside the conversation width");
            assertTrue(userMessage.displayPicture.getLayoutX()
                    + userMessage.displayPicture.getLayoutBounds().getMaxX() <= userMessage.getWidth(),
                    "The avatar must stay inside the conversation width");
            assertEquals("Command", controller.userInput.getAccessibleText());
            return null;
        });
    }

    @Test
    public void byeShowsFarewellThenClosesOnlyItsWindow(@TempDir Path tempDir) throws Exception {
        WindowFixture window = runOnFxThread(() -> createWindow(tempDir));
        Stage stage = runOnFxThread(Stage::new);
        CountDownLatch hidden = new CountDownLatch(1);
        AtomicLong submittedAt = new AtomicLong();
        AtomicLong hiddenAt = new AtomicLong();
        try {
            runOnFxThread(() -> {
                stage.setScene(window.root.getScene());
                stage.setOnHidden(event -> {
                    hiddenAt.set(System.nanoTime());
                    hidden.countDown();
                });
                stage.show();
                MainWindow controller = window.controller;
                controller.userInput.setText("bye now");
                controller.sendButton.fire();
                assertTrue(stage.isShowing());
                assertFalse(controller.userInput.isDisabled());
                assertEquals("bye now", controller.userInput.getText());
                DialogBox error = (DialogBox) controller.dialogContainer.getChildren().get(2);
                assertTrue(error.getStyleClass().contains("error-dialog"));

                submittedAt.set(System.nanoTime());
                controller.userInput.setText("  bye  ");
                controller.sendButton.fire();
                assertTrue(stage.isShowing(), "The farewell must be visible before closing");
                DialogBox farewell = (DialogBox) controller.dialogContainer.getChildren().get(4);
                assertEquals("Bye. Hope to see you again soon!\nClosing in 3 seconds.", farewell.dialog.getText());
                assertTrue(controller.userInput.isDisabled());
                assertTrue(controller.sendButton.isDisabled());
                assertTrue(controller.listButton.isDisabled());
                assertTrue(controller.sortButton.isDisabled());
                assertTrue(controller.examplesButton.isDisabled());

                controller.userInput.setText("todo Too late");
                controller.userInput.fireEvent(new javafx.event.ActionEvent());
                controller.listButton.fire();
                controller.examplesButton.fire();
                assertEquals(5, controller.dialogContainer.getChildren().size());
                assertEquals("0 tasks saved locally", controller.taskCountLabel.getText());
                return null;
            });
            assertFalse(hidden.await(1, TimeUnit.SECONDS), "Farewell closed too soon");
            assertTrue(hidden.await(10, TimeUnit.SECONDS), "The window did not close after farewell");
            assertTrue(hiddenAt.get() - submittedAt.get() >= TimeUnit.MILLISECONDS.toNanos(2800),
                    "The farewell should remain visible for three seconds");
            runOnFxThread(() -> {
                assertFalse(stage.isShowing());
                assertEquals("0 tasks saved locally", window.controller.taskCountLabel.getText());
                return null;
            });
        } finally {
            runOnFxThread(() -> {
                stage.hide();
                return null;
            });
        }
    }

    private WindowFixture createWindow(Path tempDir) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(Arn.class.getResource("/view/MainWindow.fxml"));
        AnchorPane root = loader.load();
        Scene scene = new Scene(root, 700, 760);
        scene.getStylesheets().add(Arn.class.getResource("/styles/main.css").toExternalForm());
        MainWindow controller = loader.getController();
        controller.setArn(createTestArn(tempDir));
        root.resize(700, 760);
        root.applyCss();
        root.layout();
        return new WindowFixture(root, controller);
    }

    private static class WindowFixture {
        private final AnchorPane root;
        private final MainWindow controller;

        WindowFixture(AnchorPane root, MainWindow controller) {
            this.root = root;
            this.controller = controller;
        }
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
