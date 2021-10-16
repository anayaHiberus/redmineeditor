package com.hiberus.anaya.redmineeditor.controller

import javafx.scene.control.Alert
import java.util.*

/**
 * A custom exception that allows displaying it as dialog
 *
 * @param title   with this title
 * @param message and this message
 * @param cause   and this cause
 */
class MyException(
    private val title: String,
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {

    /* ------------------------- more data ------------------------- */

    private var isWarning = false // is this exception is a warning

    private val details = StringJoiner("\n") // the exception details (different from the message)

    /* ------------------------- modifiers ------------------------- */

    /**
     * Marks this exception as warning
     *
     * @return this (for chained calls)
     */
    fun asWarning(): MyException = this.apply { isWarning = true }

    /**
     * Adds a detail whose message will be shown as a detail
     *
     * @param other another exception to add
     */
    fun addDetails(other: Exception) = details.add(other.message).run { }

    /* ------------------------- properties ------------------------- */

    /**
     * @return true iff this exception has at least a detail (addDetails was called)
     */
    fun hasDetails() = details.length() > 0

    /**
     * Displays an error dialog with this exception details
     */
    fun showAndWait() {
        print(cause)

        Alert(if (isWarning) Alert.AlertType.WARNING else Alert.AlertType.ERROR).apply {
            headerText = title
            contentText = "$message\n\n$details"
            showAndWait()
        }
    }

}