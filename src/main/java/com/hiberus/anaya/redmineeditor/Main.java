package com.hiberus.anaya.redmineeditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main app
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        // load and show
        stage.setTitle("Redmine editor");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("parent.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.show();
    }

}