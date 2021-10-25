package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.controller.settingsLoaded
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.backgroundColor
import com.hiberus.anaya.redmineeditor.utils.formatHours
import com.hiberus.anaya.redmineeditor.utils.hiberus.expectedHours
import com.hiberus.anaya.redmineeditor.utils.hiberus.getColor
import javafx.fxml.FXML
import javafx.scene.control.Label
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
            // when month, day, hours or the loading state changes, update
            setOf(ChangeEvents.Month, ChangeEvents.Day, ChangeEvents.Hours, ChangeEvents.Loading)
        ) { model: Model ->
            if (model.isLoading) {
                // while loading, notify user
                summary.text = "Loading..."
                summary.background = null
            } else if (!settingsLoaded) {
                // on settings error, notify too
                summary.text = "Can't load settings"
                summary.background = null
            } else if (!model.monthLoaded) {
                // data not loaded yet
                summary.text = "Month not loaded"
                summary.background = null
            } else model.date?.also { date ->
                // on something selected, display info
                val spent = model.getSpent(date)
                val expected = date.expectedHours
                summary.text = date.formatLong() +
                        " --- Time: ${spent.formatHours()} / ${expected.formatHours()}" +
                        when {
                            spent < expected -> " --- Missing: ${(expected - spent).formatHours()}"
                            spent > expected -> " --- Extra: ${(spent - expected).formatHours()}"
                            spent == expected && expected != 0.0 -> " --- OK"
                            else -> "" // spent=expected=0
                        }

                // and change color
                summary.backgroundColor = getColor(expected, spent, date)
            } ?: run {
                // if nothing selected, just ask
                summary.text = "Select day"
                summary.background = null
            }
        }
    }
}

/**
 * Format LocalDate as LONG
 */
private fun LocalDate.formatLong() = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))