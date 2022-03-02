package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.backgroundColor
import com.hiberus.anaya.redmineeditor.utils.expectedHours
import com.hiberus.anaya.redmineeditor.utils.formatHours
import com.hiberus.anaya.redmineeditor.utils.getColor
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.paint.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A simple label with info of the current selected day
 */
internal class SummaryComponent {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var summary: Label // the label

    /* ------------------------- actions ------------------------- */

    @FXML
    fun initialize() {
        AppController.onChanges(
            // when month, day or hours changes, update
            setOf(ChangeEvent.Month, ChangeEvent.Day, ChangeEvent.DayHours, ChangeEvent.EntryList, ChangeEvent.Loading)
        ) updating@{ model: Model ->

            val date = model.date ?: run {
                // if nothing selected, just ask
                set("Select day")
                return@updating
            }
            val spent = model.getSpent(date) ?: run {
                // data not loaded yet, maybe loading
                set(if (model.isLoading) "Loading..." else "No data")
                return@updating
            }
            val expected = date.expectedHours

            // on something selected, display info with color
            set(describe(date, spent, expected), getColor(expected, spent, date))
        }
    }

    /**
     * Just for easier setters
     */
    fun set(label: String, color: Color? = null) = summary.apply { text = label; backgroundColor = color }

}


/**
 * Get the description of a [date] with [spent] and [expected] hours
 */
fun describe(date: LocalDate, spent: Double, expected: Double) =
    date.formatLong() +
            " --- Time: ${spent.formatHours()} / ${expected.formatHours()}" +
            when {
                spent < expected -> " --- Missing: ${(expected - spent).formatHours()}"
                spent > expected -> " --- Extra: ${(spent - expected).formatHours()}"
                spent == expected && expected != 0.0 -> " --- OK"
                else -> "" // spent=expected=0
            }

/**
 * Format LocalDate as LONG
 */
private fun LocalDate.formatLong() = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))