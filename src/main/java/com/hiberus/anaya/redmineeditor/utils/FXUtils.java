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

import java.util.concurrent.CountDownLatch;

/**
 * Utilities to use JavaFX even better
 */
public class FXUtils {

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
     * Runs something in UI thread, then continues.
     * If this is called from the UI thread, it is run directly
     * If this is called from a background thread, it is scheduled to run in UI thread (using {@link Platform#runLater(Runnable)}) and waits until it finishes
     *
     * @param run code to run in foreground before this function ends
     */
    public static void runInForeground(Runnable run) {
        if (Platform.isFxApplicationThread()) {
            // already in foreground, run directly
            run.run();
        } else {
            // in background, schedule and wait
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    run.run();
                } finally {
                    // finished
                    latch.countDown();
                }
            });
            // wait until it finishes
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        }
    }

}
