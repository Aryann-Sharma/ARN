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
    protected Label dialog;
    @FXML
    protected ImageView displayPicture;

    public DialogBox(String text, Image img, String roleStyleClass) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
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

    public static DialogBox getUserDialog(String s, Image i) {
        return new DialogBox(s, i, "user-dialog");
    }

    public static DialogBox getArnDialog(String s, Image i) {
        var db = new DialogBox(s, i, "arn-dialog");
        db.flip();
        return db;
    }

    public static DialogBox getErrorDialog(String s, Image i) {
        var db = new DialogBox(s, i, "error-dialog");
        db.flip();
        return db;
    }
}
