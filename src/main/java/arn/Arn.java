package arn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
 * Main entry point for Arn application
 * <p>
 * Initializes the user interface, loads tasks from
 * storage, and processes user commands until termination.
 */
public class Arn extends Application {
    private static final Logger LOGGER = Logger.getLogger(Arn.class.getName());

    TaskFileHandler taskFileHandler;
    TaskList taskList;
    Gui gui;
    Parser parser;

    public static void main(String[] args) {
        Ui ui = new Ui();
        ui.displayGreet();

        TaskFileHandler taskFileHandler = new TaskFileHandler("./data/arn.txt");
        TaskList taskList;
        try {
            taskList = new TaskList(taskFileHandler.readTasks());
        } catch (StorageException e) {
            LOGGER.log(Level.SEVERE, "Unable to start because task data could not be loaded", e);
            ui.displayMsg("Error: " + e.getMessage());
            ui.close();
            return;
        }

        Parser parser = new Parser(taskList, ui);

        while (true) {
            String input = ui.readCommand();
            List<Task> previousTasks = taskList.getTasks();
            List<Boolean> previousStatuses = getTaskStatuses(previousTasks);
            try {
                parser.parse(input);
                taskFileHandler.writeTasks(taskList.getTasks());
                if ("bye".equals(input)) {
                    break;
                }
            } catch (ArnException e) {
                ui.displayMsg("Error: " + e.getMessage());
            } catch (StorageException e) {
                taskList = restoreTasks(previousTasks, previousStatuses);
                parser = new Parser(taskList, ui);
                ui.displayMsg("Error: " + e.getMessage());
            }

            ui.displayMsg("");
        }

        ui.close();
    }

    @Override
    public void start(Stage stage) {
        try {
            taskFileHandler = new TaskFileHandler("./data/arn.txt");
            taskList = new TaskList(taskFileHandler.readTasks());
            gui = new Gui();
            parser = new Parser(taskList, gui);

            FXMLLoader fxmlLoader = new FXMLLoader(Arn.class.getResource("/view/MainWindow.fxml"));
            AnchorPane ap = fxmlLoader.load();
            Scene scene = new Scene(ap);
            scene.getStylesheets().add(Arn.class.getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Arn — Your task assistant");
            stage.getIcons().add(new Image(Arn.class.getResourceAsStream("/images/ArnLogo.png")));
            stage.setMinWidth(560);
            stage.setMinHeight(640);
            fxmlLoader.<MainWindow>getController().setArn(this);
            stage.show();
        } catch (IOException | StorageException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unable to start Arn", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to start Arn");
            alert.setHeaderText("Arn could not start");
            alert.setContentText("Check that the data directory is accessible, then try again.");
            alert.showAndWait();
            Platform.exit();
        }
    }

    public String getResponse(String input) {
        List<Task> previousTasks = taskList.getTasks();
        List<Boolean> previousStatuses = getTaskStatuses(previousTasks);
        try {
            parser.parse(input);
            taskFileHandler.writeTasks(taskList.getTasks());
            return gui.getResponses();
        } catch (ArnException e) {
            gui.clearResponses();
            return "Error: " + e.getMessage();
        } catch (StorageException e) {
            taskList = restoreTasks(previousTasks, previousStatuses);
            parser = new Parser(taskList, gui);
            gui.clearResponses();
            return "Error: " + e.getMessage();
        }
    }

    private static List<Boolean> getTaskStatuses(List<Task> tasks) {
        List<Boolean> statuses = new ArrayList<>();
        for (Task task : tasks) {
            statuses.add(task.isDone());
        }
        return statuses;
    }

    private static TaskList restoreTasks(List<Task> tasks, List<Boolean> statuses) {
        for (int i = 0; i < tasks.size(); i++) {
            if (statuses.get(i)) {
                tasks.get(i).markAsDone();
            } else {
                tasks.get(i).markAsNotDone();
            }
        }
        return new TaskList(tasks);
    }

    public int getTaskCount() {
        return taskList.size();
    }
}
