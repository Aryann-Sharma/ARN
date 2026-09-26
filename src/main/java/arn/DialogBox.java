package arn;
import java.io.IOException;
import java.util.Collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class DialogBox extends HBox {

    @FXML
    Label dialog;
    @FXML
    ImageView displayPicture;

    public DialogBox(String text, Image img, String roleStyleClass) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the dialog layout.", e);
        }

        dialog.setText(text);
        displayPicture.setImage(img);
        getStyleClass().add(roleStyleClass);
        setAccessibleText(text);
    }

    public void flip() {
        ObservableList<Node> tmp = FXCollections.observableArrayList(this.getChildren());
        Collections.reverse(tmp);
        getChildren().setAll(tmp);
        setAlignment(Pos.TOP_LEFT);
    }

    public static DialogBox getUserDialog(String text, Image image) {
        return new DialogBox(text, image, "user-dialog");
    }

    public static DialogBox getArnDialog(String text, Image image) {
        DialogBox dialogBox = new DialogBox(text, image, "arn-dialog");
        dialogBox.flip();
        return dialogBox;
    }

    public static DialogBox getErrorDialog(String text, Image image) {
        DialogBox dialogBox = new DialogBox(text, image, "error-dialog");
        dialogBox.flip();
        return dialogBox;
    }
}
