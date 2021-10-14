package com.hiberus.anaya.redmineeditor.utils

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import java.util.concurrent.CountDownLatch

/**
 * Utilities to use JavaFX even better
 */
object FXUtils {

    /**
     * Sets the background color of a region
     *
     * @param node  region to change
     * @param color color to set
     */
    @JvmStatic
    fun setBackgroundColor(node: Region, color: Color?) {
        node.background = color?.let { Background(BackgroundFill(it, CornerRadii(5.0), Insets(1.0))) }
    }

    /**
     * Creates a new Label that will have its content centered
     *
     * @param text initial label text
     * @return the created centered label
     */
    @JvmStatic
    fun getCenteredLabel(text: String): Label =
        Label(text).apply {
            maxWidth = Double.MAX_VALUE
            alignment = Pos.CENTER
        }

    /**
     * Runs something in UI thread, then continues.
     * If this is called from the UI thread, it is run directly
     * If this is called from a background thread, it is scheduled to run in UI thread (using [Platform.runLater]) and waits until it finishes
     *
     * @param run code to run in foreground before this function ends
     */
    @JvmStatic
    fun runInForeground(run: Runnable) =
        if (Platform.isFxApplicationThread()) {
            // already in foreground, run directly
            run.run()
        } else {
            // in background, schedule and wait
            val latch = CountDownLatch(1)
            Platform.runLater {
                try {
                    run.run()
                } finally {
                    // finished
                    latch.countDown()
                }
            }
            // wait until it finishes
            try {
                latch.await()
            } catch (ignored: InterruptedException) {
            }
        }
}