package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.FXUtils.setBackgroundColor
import com.hiberus.anaya.redmineeditor.utils.TimeUtils.formatHours
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule.getColor
import com.hiberus.anaya.redmineeditor.utils.hiberus.Schedule.getExpectedHours
import javafx.fxml.FXML
import javafx.scene.control.Label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A simple label with info of the current selected day
 */
internal class SummaryComponent : BaseComponent() {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var summary: Label // the label

    /* ------------------------- actions ------------------------- */

    public override fun init() = controller.register(
        // when month, day, hours or the loading state changes, update
        setOf(ChangeEvents.Month, ChangeEvents.Day, ChangeEvents.Hours, ChangeEvents.Loading)
    ) { model: Model ->
        if (model.isLoading) {
            // while loading, notify user
            summary.text = "Loading..."
            summary.background = null
            return@register
        }
        // not loading, display
        model.date?.also { date ->
            // on something selected, display info
            val spent = model.getSpent(date)
            val expected = getExpectedHours(date)
            summary.text = date.formatLong() +
                    " --- Time: ${formatHours(spent)} / ${formatHours(expected)}" +
                    when {
                        spent < expected -> " --- Missing: ${formatHours(expected - spent)}"
                        spent > expected -> " --- Extra: ${formatHours(spent - expected)}"
                        spent == expected && expected != 0.0 -> " --- OK"
                        else -> "" // spent=expected=0
                    }

            // and change color
            setBackgroundColor(summary, getColor(expected, spent, date))
        } ?: run {
            // if nothing selected, just ask
            summary.text = "Select day"
            summary.background = null
        }
    }
}

/**
 * Format LocalDate as LONG
 */
private fun LocalDate.formatLong() = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))