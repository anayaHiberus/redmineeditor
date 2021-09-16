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

/**
 * Utilities to use JavaFX even better
 */
public class JavaFXUtils {

    /**
     * Sets the background color of a region
     *
     * @param node  region to change
     * @param color color to set
     */
    static public void setBackgroundColor(Region node, Color color) {
        if (color == null) node.setBackground(null);
        else node.setBackground(new Background(new BackgroundFill(color, new CornerRadii(5.0), new Insets(1))));
    }

    /**
     * Creates a new Label that will have its content centered
     *
     * @param text initial label text
     * @return the created centered label
     */
    public static Label getCenteredLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    /**
     * Runs something in the background, then notify on foreground
     *
     * @param background something to run in background
     * @param foreground something to run in foreground AFTER background finishes
     */
    public static void runInBackground(Runnable background, Runnable foreground) {
        new Thread(() -> {
            background.run();
            Platform.runLater(foreground);
        }).start();
    }
}
