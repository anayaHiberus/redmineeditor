package com.hiberus.anaya.redmineeditor.utils

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.DeprecationLevel.ERROR

/**
 * The background color of this region (null for no color)
 * Setter only (https://youtrack.jetbrains.com/issue/KT-6519#focus=Comments-27-3525647.0-0)
 */
var Region.backgroundColor: Color?
    @Deprecated("", level = ERROR) get() = throw UnsupportedOperationException()
    set(value) {
        // create a background with that color and rounded borders
        background = value?.let { Background(BackgroundFill(it, CornerRadii(5.0), Insets(1.0))) }
    }

/**
 * Creates a new Label that will have its content centered
 *
 * @param text initial label text
 * @return the created centered label
 */
fun CenteredLabel(text: String) = Label(text).apply {
    maxWidth = Double.MAX_VALUE
    alignment = Pos.CENTER
}

/**
 * Makes the node gone (no space) when invisible, otherwise it's just invisible
 */
fun Node.syncInvisible() = managedProperty().bind(visibleProperty())

/**
 * Runs something in UI thread, then continues.
 * If this is called from the UI thread, it is run directly
 * If this is called from a background thread, it is scheduled to run in UI thread (using [Platform.runLater]) and waits until it finishes
 *
 * @param function code to run in foreground before this function ends
 */
fun runInForeground(function: () -> Unit) =
    if (Platform.isFxApplicationThread()) {
        // already in foreground, run directly
        function()
    } else {
        // in background, schedule and wait
        val latch = CountDownLatch(1)
        Platform.runLater {
            try {
                function()
            } finally {
                // finished
                latch.countDown()
            }
        }
        // wait until it finishes
        latch.await()
    }

/**
 * The result button, null if not present
 */
val Optional<ButtonType>.resultButton
    get() = this.takeIf { it.isPresent }?.get()