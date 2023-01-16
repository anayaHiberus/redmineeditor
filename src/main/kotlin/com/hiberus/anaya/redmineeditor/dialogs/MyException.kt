package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineeditor.utils.debugln
import com.hiberus.anaya.redmineeditor.utils.letEach
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.ERROR
import javafx.scene.control.Alert.AlertType.WARNING
import javafx.scene.control.Label
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
     * Adds an exception whose message will be shown as a detail
     *
     * @param other another exception to add
     */
    fun addDetail(other: Throwable) = details.add(other.message).run { debugln(other) }

    /**
     * Adds exceptions whose message will be shown as a detail
     *
     * @param other other exceptions to add
     */
    fun addDetails(other: Iterable<Throwable>) = other.letEach { details.add(message) }

    /* ------------------------- properties ------------------------- */

    /**
     * Displays an error dialog with this exception details
     */
    fun showAndWait() {
        Alert(if (warning) WARNING else ERROR).apply {
            headerText = title
            contentText = "$message\n\n$details"
            cause?.let { cause ->
                debugln(cause)
                dialogPane.expandableContent = Label(cause.toString()) // show error details
            }
            stylize()
        }.showAndWait()
    }

}

/**
 * Wraps this throwable into a MyException generated by [generator], unless it is one already (in which case [generator] is not called)
 */
fun Throwable.convert(generator: () -> MyException) = when (this) {
    // if already a MyException, return it
    is MyException -> this
    // otherwise, convert
    else -> generator().also { it.addDetail(this) }
}

/**
 * Converts this list of exceptions into a MyException generated by [generator], unless the list is empty
 */
fun List<Throwable>.convert(generator: () -> MyException) =
    if (isEmpty()) null
    else generator().also {
        it.addDetails(this)
    }