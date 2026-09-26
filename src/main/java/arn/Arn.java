package arn;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Main entry point for Arn application
 * <p>
 * Initializes the user interface, loads tasks from
 * storage, and processes user commands until termination.
 */
public class Arn extends Application {
    TaskFileHandler taskFileHandler;
    TaskList taskList;
    Gui gui;
    Parser parser;

    public static void main(String[] args) {
        Ui ui = new Ui();
        ui.displayGreet();

        TaskFileHandler taskFileHandler = new TaskFileHandler("./data/arn.txt");
        TaskList taskList = new TaskList(taskFileHandler.readTasks());
        Parser parser = new Parser(taskList, ui);
        String input = "";

        while(true) {
            input = ui.readCommand();
            try {
                parser.parse(input);
                if (input.equals("bye")) {
                    break;
                }
            } catch (ArnException e) {
                ui.displayMsg("Error: " + e.getMessage());
            }

            taskFileHandler.writeTasks(taskList.get());
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getResponse(String input) {
        try {
            parser.parse(input);
            taskFileHandler.writeTasks(taskList.get());
            return gui.getResponses();
        } catch (ArnException e) {
            return "Error: " + e.getMessage();
        }
    }

    public int getTaskCount() {
        return taskList.size();
    }
}
