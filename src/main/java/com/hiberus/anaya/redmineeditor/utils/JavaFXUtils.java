package com.hiberus.anaya.redmineeditor.utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class JavaFXUtils {

    static public void setBackgroundColor(Region node, Color color) {
        node.setBackground(new Background(new BackgroundFill(color, new CornerRadii(5.0), new Insets(1))));
    }

    public static Label getCenteredLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    public static void runInBackground(Runnable background, Runnable foreground) {
        new Thread(() -> {
            background.run();
            Platform.runLater(foreground);
        }).start();
    }
}
