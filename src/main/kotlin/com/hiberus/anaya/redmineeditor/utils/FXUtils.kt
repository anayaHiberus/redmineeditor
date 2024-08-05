package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.HEADLESS
import com.hiberus.anaya.redmineeditor.model.multiplyOpacity
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.paint.*
import javafx.stage.Screen
import javafx.stage.Window
import javafx.stage.WindowEvent
import java.awt.MouseInfo
import java.net.URI
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.DeprecationLevel.ERROR
import kotlin.math.min


/**
 * The background plain color of this region (null for no color)
 * Setter only
 */
inline var Region.backgroundColor: Color?
    @Deprecated("", level = ERROR) get() = throw UnsupportedOperationException() // https://youtrack.jetbrains.com/issue/KT-6519#focus=Comments-27-3525647.0-0
    set(value) {
        indicatorColor(background = value)
    }

/** Sets the background color of this region with some indicators with other colors */
fun Region.indicatorColor(background: Color? = null, topLeft: Color? = null, verticalBars: Color? = null) {
    if (background == null && topLeft == null && verticalBars == null) {
        // early null check
        this.background = null
        return
    }

    // create a background list with a color and special indicators
    buildList {
        // plain background
        if (background != null) add(
            background.multiplyOpacity(0.75)
        )

        // round top-left corner
        if (topLeft != null) add(
            RadialGradient(
                0.0, 0.0, 1.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE, listOf(
                    Stop(0.9, Color.TRANSPARENT),
                    Stop(1.0, topLeft),
                )
            )
        )

        // vertical left+right bars
        if (verticalBars != null) add(
            LinearGradient(
                0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE, listOf(
                    Stop(0.1, verticalBars),
                    Stop(0.2, Color.TRANSPARENT),
                    Stop(0.8, Color.TRANSPARENT),
                    Stop(0.9, verticalBars),
                )
            )
        )
    }
        // and rounded corners
        .map { BackgroundFill(it, CornerRadii(5.0), Insets(1.0)) }
        .let { this.background = Background(*it.toTypedArray()) }
}

/** Bold style of a region (for its text) */
var Region.bold: Boolean
    get() = BOLD_STYLE in (style ?: "")
    set(value) {
        style = if (value) BOLD_STYLE else null
    }
private const val BOLD_STYLE = "-fx-font-weight: bold;"

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

/** Makes the node gone (no space) when invisible, otherwise it's just invisible */
fun Node.syncInvisible() = managedProperty().bind(visibleProperty())

/**
 * The enable/disable state of a node.
 * JavaFX contains two properties: isDisable specifies if a node is marked as disabled. isDisabled specifies if a node is effectively disabled (because either itself is disabled or a parent is disabled)
 * This property was created to flip the logic to an enabled property (I personally prefer it) and also to mix both properties:
 * - getter: If true, the button is enabled, if false the button or a prent is disabled (!isDisabled)
 * - setter: Sets the isDisable property (negated)
 * This means that after enable = true, enable can still return false (if a parent is disabled)
 */
inline var Node.enabled
    get() = !isDisabled
    set(value) {
        isDisable = !value
    }

/**
 * Runs something in UI thread, then continues.
 * If this is called from the UI thread, it is run directly
 * If this is called from a background thread, it is scheduled to run in UI thread (using [Platform.runLater]) and waits until it finishes
 *
 * @param function code to run in foreground before this function ends
 */
fun runInForeground(function: () -> Unit) =
    if (HEADLESS or Platform.isFxApplicationThread()) {
        // headless mode or already in foreground, run directly
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

/** The result button, null if not present */
inline val Optional<ButtonType>.resultButton
    get() = this.takeIf { it.isPresent }?.get()


/**
 * Displays a dialog to confirm losing changes and [message]
 * returns true if the user accepted (so it is ok to lose changes)
 */
fun confirmLoseChanges(message: String): Boolean {

    return Alert(
        Alert.AlertType.WARNING,
        "There are unsaved changes, do you want to lose them and $message?",
        ButtonType.YES, ButtonType.CANCEL
    ).apply {
        title = "Warning"
        headerText = "Unsaved changes"
        stylize()
    }.showAndWait()
        // return whether the user accepted
        .resultButton == ButtonType.YES
}

/** Removes all buttons of this alert */
fun Alert.clearButtons() = buttonTypes.clear()

/**
 * Adds and returns a button with a custom listener.
 * If the button exists it's not duplicated, only the listener is updated
 */
fun Alert.addButton(button: ButtonType, listener: () -> Unit = {}): Button {
    if (button !in buttonTypes) buttonTypes += button
    return (dialogPane.lookupButton(button) as Button).apply {
        setOnAction {
            listener()
            it.consume()
        }
    }
}

/** Tries to open an url in the browser, displays an alert if fails */
fun openInBrowser(url: String) {
    daemonThread {
        URI(url).openInBrowser().ifNotOK {
            // on error, display alert for manual open
            Platform.runLater {
                TextInputDialog(url).apply {
                    title = "Browse url"
                    headerText = "Couldn't open the browser automatically, here is the url for manual opening:"

                    stylize()
                    dialogPane.buttonTypes.remove(ButtonType.CANCEL) // unused, we don't care for the result
                }.showAndWait()
            }
        }
    }
}

/**
 * Centers the window on the mouse screen, and reduces its size if it's bigger
 * Coded as a callback, can be called before the window is shown
 */
fun Window.centerInMouseScreen() {
    addEventHandler(WindowEvent.WINDOW_SHOWN) {
        MouseInfo.getPointerInfo()?.location?.run {
            Screen.getScreensForRectangle(x.toDouble(), y.toDouble(), 1.0, 1.0).getOrNull(0)?.bounds
        }?.also {
            width = min(width, it.width * 0.9)
            height = min(height, it.height * 0.9)

            x = (it.minX + it.maxX) / 2 - width / 2
            y = (it.minY + it.maxY) / 2 - height / 2
        }
    }
}
