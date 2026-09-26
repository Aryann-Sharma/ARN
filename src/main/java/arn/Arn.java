package arn;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Starts the application and saves successful changes before reporting them.
 */
public class Arn extends Application {
    private static final Logger LOGGER = Logger.getLogger(Arn.class.getName());
    private static final int UNDO_LIMIT = 100;

    TaskFileHandler taskFileHandler;
    TaskList taskList;
    Gui gui;
    Parser parser;
    private boolean exitRequested;
    private final Deque<TaskSnapshot> undoHistory = new ArrayDeque<>();

    public static void main(String[] args) {
        int exitCode = runConsole(new Ui());
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int runConsole(Ui ui) {
        try (ui) {
            ui.displayGreet();
            Arn arn = new Arn();
            arn.initialize(new TaskFileHandler("./data/arn.txt"));
            String input;
            while ((input = ui.readCommand()) != null) {
                ui.displayMsg(arn.getResponse(input));
                if (arn.isExitRequested()) {
                    break;
                }
                ui.displayMsg("");
            }
        } catch (StorageException e) {
            LOGGER.log(Level.FINE, "Unable to start because task data could not be loaded", e);
            ui.displayMsg("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    void initialize(TaskFileHandler storage) throws StorageException {
        exitRequested = false;
        undoHistory.clear();
        taskFileHandler = storage;
        taskList = new TaskList(storage.readTasks());
        gui = new Gui();
        parser = new Parser(taskList, gui);
    }

    @Override
    public void start(Stage stage) {
        try {
            initialize(new TaskFileHandler("./data/arn.txt"));

            FXMLLoader fxmlLoader = new FXMLLoader(Arn.class.getResource("/view/MainWindow.fxml"));
            AnchorPane ap = fxmlLoader.load();
            Scene scene = new Scene(ap);
            scene.getStylesheets().add(Arn.class.getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Arn — Your task assistant");
            String iconUrl = Arn.class.getResource("/images/ArnTaskbar.png").toExternalForm();
            for (int size : new int[] {16, 32, 48, 64, 128, 256}) {
                stage.getIcons().add(new Image(iconUrl, size, size, true, true));
            }
            stage.setMinWidth(560);
            stage.setMinHeight(640);
            fxmlLoader.<MainWindow>getController().setArn(this);
            stage.show();
        } catch (IOException | StorageException | RuntimeException e) {
            LOGGER.log(e instanceof StorageException ? Level.FINE : Level.SEVERE, "Unable to start Arn", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to start Arn");
            alert.setHeaderText("Arn could not start");
            alert.setContentText(e instanceof StorageException
                    ? e.getMessage()
                    : "The application could not load its interface. Try downloading the release again, "
                            + "or use java -jar Arn.jar --cli to open the console.");
            alert.showAndWait();
            Platform.exit();
        }
    }

    public String getResponse(String input) {
        exitRequested = false;
        gui.clearResponses();
        TaskSnapshot previousTasks = new TaskSnapshot(taskList);
        try {
            parser.parse(input);
            if (gui.isUndoRequested()) {
                if (undoHistory.isEmpty()) {
                    throw new ArnException("Nothing to undo. Undo is available after a saved change in this session.");
                }
                taskList = undoHistory.peek().restore();
                taskFileHandler.writeTasks(taskList.getTasks());
                undoHistory.pop();
                parser = new Parser(taskList, gui);
                gui.displayMsg("Undid the last change. Use 'list' to see your tasks.");
            } else if (!previousTasks.matches(taskList)) {
                taskFileHandler.writeTasks(taskList.getTasks());
                undoHistory.push(previousTasks);
                if (undoHistory.size() > UNDO_LIMIT) {
                    undoHistory.removeLast();
                }
            }
            exitRequested = gui.isExitRequested();
            return gui.getResponses();
        } catch (ArnException | StorageException e) {
            taskList = previousTasks.restore();
            parser = new Parser(taskList, gui);
            gui.clearResponses();
            return "Error: " + e.getMessage();
        }
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    public int getTaskCount() {
        return taskList.size();
    }
}
