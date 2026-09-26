package arn;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Opens the desktop from the packaged resources in an isolated process. */
public final class PackagedGuiProbe {
    private static Throwable failure;
    private static boolean closedAfterBye;

    public static void main(String[] args) {
        Application.launch(Window.class, args);
        if (failure == null && !closedAfterBye) {
            failure = new AssertionError("The desktop did not close through its bye command");
        }
        if (failure != null) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    public static final class Window extends Application {
        @Override
        public void start(Stage stage) {
            try {
                new Arn().start(stage);
                require(stage.isShowing(), "Desktop did not open");
                Platform.runLater(() -> verifyWindow(stage));
            } catch (Throwable error) {
                failure = error;
                Platform.exit();
            }
        }

        private void verifyWindow(Stage stage) {
            try {
                TextField input = (TextField) stage.getScene().lookup("#userInput");
                Button send = (Button) stage.getScene().lookup("#sendButton");
                Label count = (Label) stage.getScene().lookup("#taskCountLabel");
                input.setText("todo Review the desktop release");
                send.fire();
                require("1 task saved locally".equals(count.getText()), "Task count was not updated");
                require(input.getText().isEmpty(), "Successful command was not cleared");
                input.setText("edit 1 Review task editing");
                send.fire();
                require(Files.readString(Path.of("data/arn.txt")).contains("Review task editing"),
                        "Desktop edit was not saved");
                input.setText("undo");
                send.fire();
                require(Files.readString(Path.of("data/arn.txt")).contains("Review the desktop release"),
                        "Desktop undo did not restore the description");
                input.setText("deadline Invalid date /by 2026-02-30");
                send.fire();
                require(!input.getText().isEmpty(), "Invalid command was not retained for correction");
                require(Files.readString(Path.of("data/arn.txt")).contains("Review the desktop release"),
                        "Desktop command was not saved");
                Path output = Path.of(getParameters().getRaw().get(0));
                Files.createDirectories(output);
                snapshot(stage, output.resolve("desktop.png"));
                stage.setWidth(560);
                stage.setHeight(640);
                Platform.runLater(() -> {
                    try {
                        snapshot(stage, output.resolve("desktop-small.png"));
                        verifyFarewell(stage, output);
                    } catch (Throwable error) {
                        failure = error;
                        stage.close();
                        Platform.exit();
                    }
                });
            } catch (Throwable error) {
                failure = error;
                stage.close();
                Platform.exit();
            }
        }

        private void verifyFarewell(Stage stage, Path output) {
            TextField input = (TextField) stage.getScene().lookup("#userInput");
            Button send = (Button) stage.getScene().lookup("#sendButton");
            input.setText("bye now");
            send.fire();
            require(stage.isShowing() && !input.isDisabled(), "Malformed bye must leave the desktop usable");
            require("bye now".equals(input.getText()), "Malformed bye should remain editable");

            long submittedAt = System.nanoTime();
            stage.setOnHidden(event -> {
                try {
                    require(System.nanoTime() - submittedAt >= TimeUnit.MILLISECONDS.toNanos(2800),
                            "The desktop closed before the three-second farewell finished");
                    closedAfterBye = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            input.setText("bye");
            send.fire();
            require(stage.isShowing(), "The farewell should be visible before closing");
            require(input.isDisabled() && send.isDisabled(), "Command input must be disabled while exiting");
            for (String id : new String[] {"listButton", "sortButton", "examplesButton"}) {
                require(stage.getScene().lookup("#" + id).isDisabled(), "Quick actions must be disabled while exiting");
            }
            VBox conversation = (VBox) stage.getScene().lookup("#dialogContainer");
            DialogBox farewell = (DialogBox) conversation.getChildren().get(conversation.getChildren().size() - 1);
            require(farewell.dialog.getText().equals("Bye. Hope to see you again soon!\nClosing in 3 seconds."),
                    "Desktop farewell or closing notice is missing");

            PauseTransition visibleCheck = new PauseTransition(Duration.seconds(1));
            visibleCheck.setOnFinished(event -> {
                try {
                    require(stage.isShowing(), "The farewell must remain visible for more than one second");
                    snapshot(stage, output.resolve("desktop-farewell.png"));
                } catch (Throwable error) {
                    failure = error;
                    stage.close();
                    Platform.exit();
                }
            });
            visibleCheck.play();

            PauseTransition timeout = new PauseTransition(Duration.seconds(8));
            timeout.setOnFinished(event -> {
                failure = new AssertionError("The desktop did not exit after bye");
                stage.close();
                Platform.exit();
            });
            timeout.play();
        }

        private void snapshot(Stage stage, Path output) throws Exception {
            stage.getScene().getRoot().applyCss();
            stage.getScene().getRoot().layout();
            WritableImage image = stage.getScene().snapshot(null);
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            int[] pixels = new int[width * height];
            image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);
            require(ImageIO.write(bufferedImage, "png", output.toFile()), "Screenshot could not be written");
        }

        private void require(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
