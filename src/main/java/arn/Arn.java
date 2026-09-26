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
 * Starts the application and saves successful changes before reporting them.
 */
public class Arn extends Application {
    private static final Logger LOGGER = Logger.getLogger(Arn.class.getName());

    TaskFileHandler taskFileHandler;
    TaskList taskList;
    Gui gui;
    Parser parser;

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
                if ("bye".equals(input.strip())) {
                    break;
                }
                ui.displayMsg("");
            }
        } catch (StorageException e) {
            LOGGER.log(Level.SEVERE, "Unable to start because task data could not be loaded", e);
            ui.displayMsg("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    void initialize(TaskFileHandler storage) throws StorageException {
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
            alert.setContentText(e instanceof StorageException
                    ? e.getMessage()
                    : "The application could not load its interface. Try downloading the release again.");
            alert.showAndWait();
            Platform.exit();
        }
    }

    public String getResponse(String input) {
        gui.clearResponses();
        List<Task> previousTasks = taskList.getTasks();
        List<Boolean> previousStatuses = getTaskStatuses(previousTasks);
        try {
            parser.parse(input);
            if (hasChanges(previousTasks, previousStatuses)) {
                taskFileHandler.writeTasks(taskList.getTasks());
            }
            return gui.getResponses();
        } catch (ArnException | StorageException e) {
            taskList = restoreTasks(previousTasks, previousStatuses);
            parser = new Parser(taskList, gui);
            gui.clearResponses();
            return "Error: " + e.getMessage();
        }
    }

    private boolean hasChanges(List<Task> previousTasks, List<Boolean> previousStatuses) {
        return !previousTasks.equals(taskList.getTasks())
                || !previousStatuses.equals(getTaskStatuses(taskList.getTasks()));
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
