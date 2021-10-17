package com.hiberus.anaya.redmineeditor.controller

import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.ERROR
import javafx.scene.control.Alert.AlertType.WARNING
import java.util.*

/**
 * A custom exception that allows displaying it as dialog
 *
 * @param title   with this title
 * @param message and this message
 * @param cause   and optionally this cause
 * @param warning and optionally marked as warning
 */
class MyException(
    private val title: String,
    message: String,
    cause: Throwable? = null,
    private var warning: Boolean = false,
) : Exception(message, cause) {

    /* ------------------------- more data ------------------------- */

    private val details = StringJoiner("\n") // the exception details (different from the message)

    /* ------------------------- modifiers ------------------------- */

    /**
     * Adds a detail whose message will be shown as a detail
     *
     * @param other another exception to add
     */
    fun addDetails(other: MyException) = details.add(other.message).run { }

    /* ------------------------- properties ------------------------- */

    /**
     * @return true iff this exception has at least a detail (addDetails was called)
     */
    fun hasDetails() = details.length() > 0

    /**
     * Displays an error dialog with this exception details
     */
    fun showAndWait() {
        print(cause) // TODO: add in the alert as scrollable text or something

        Alert(if (warning) WARNING else ERROR).apply {
            headerText = title
            contentText = "$message\n\n$details"
        }.showAndWait()
    }

}


