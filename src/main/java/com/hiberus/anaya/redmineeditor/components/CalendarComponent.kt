package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.controller.MyException
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.CenteredLabel
import com.hiberus.anaya.redmineeditor.utils.backgroundColor
import com.hiberus.anaya.redmineeditor.utils.formatHours
import com.hiberus.anaya.redmineeditor.utils.hiberus.expectedHours
import com.hiberus.anaya.redmineeditor.utils.hiberus.getColor
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import java.time.DayOfWeek
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.*

/**
 * A calendar view with colored days
 */
internal class CalendarComponent {

    /* ------------------------- properties ------------------------- */

    /**
     * the currently displayed labels
     */
    private val days = arrayOfNulls<Label>(31)

    /**
     * the selected day index from [days]
     */
    private var selected = -1

    /**
     * to draw colors after month loads
     */
    private var needsColoring = false

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var calendarLabel: Label // month/year label

    @FXML
    lateinit var calendar: GridPane // grid

    /* ------------------------- init ------------------------- */

    @FXML
    private fun initialize() {
        // create the header
        DayOfWeek.values().forEach {
            // append each day
            calendar.add(CenteredLabel(
                it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            ), it.value - 1, 0)
        }

        // on new month, draw it and prepare to draw colors
        AppController.onChanges(setOf(ChangeEvents.Month)) { model: Model ->
            drawGrid(model)
            updateLabel(model)
            needsColoring = true
        }

        // when hours change, recolor today
        AppController.onChanges(setOf(ChangeEvents.Hours)) { model: Model ->
            // when hours change (and a recoloring is not pending), recolor day
            if (!needsColoring) model.day?.let { colorDay(it, model) }
        }

        // when finished loading, color days
        AppController.onChanges(setOf(ChangeEvents.Month, ChangeEvents.Loading)) { model: Model ->
            // if it's not loading and a recoloring is pending: color days
            if (!model.isLoading) {
                if (needsColoring) {
                    colorDays(model)
                    updateLabel(model)
                }
                needsColoring = false
            }
        }

        // when day changes (or month), set selection
        AppController.onChanges(setOf(ChangeEvents.Day, ChangeEvents.Month)) { model: Model ->
            // unselect
            unselectDay()
            // select new (if there is a selection)
            model.day?.let {
                selected = it - 1
                days[selected]?.border = Border(BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii(5.0), BorderWidths(1.0)))
            }
        }
    }

    /* ------------------------- actions ------------------------- */

    @FXML
    private fun onNextMonth() = AppController.runBackground { model: Model.Editor ->
        // next month
        loadMonth(1, model)
    }

    @FXML
    private fun onPreviousMonth() = AppController.runBackground { model: Model.Editor ->
        // previous month
        loadMonth(-1, model)
    }

    /* ------------------------- internal ------------------------- */

    @Throws(MyException::class)
    private fun loadMonth(offset: Int, model: Model.Editor) = with(model) {
        // change month by offset
        month = month.plusMonths(offset.toLong())

        // unselect the day
        day = null

        AppController.fireChanges() // notify now to display month in UI

        // and load month
        loadDate()
    }

    /**
     * color all days of current month
     */
    private fun colorDays(model: Model) =
        (1..model.month.lengthOfMonth())
            .forEach { colorDay(it, model) }

    /**
     * color a single day of current month
     */
    private fun colorDay(day: Int, model: Model) =
        model.month.atDay(day).let { date ->
            days[day - 1]?.let { label ->
                label.backgroundColor = model.getSpent(date)?.let { getColor(date.expectedHours, it, date) }
            }
        }

    private fun updateLabel(model: Model) {
        // month info
        var label = DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR)
            .appendLiteral(", ")
            .appendText(ChronoField.YEAR)
            .toFormatter()
            .format(model.month)

        val spent = model.getSpent(model.month)
        if (!model.isLoading && spent != null) {
            // loaded, append spent/expected and set color
            val expected = model.month.expectedHours
            label += " (${spent.formatHours()}/${expected.formatHours()})"
            calendarLabel.backgroundColor = getColor(expected, spent, model.month.atEndOfMonth())
        } else {
            // not loaded, clear
            calendarLabel.backgroundColor = null
        }

        // set
        calendarLabel.text = label
    }

    private fun drawGrid(model: Model) {
        // clear
        calendar.children.removeAll(days)
        days.fill(null)
        unselectDay()

        // draw month
        val month = model.month
        val padding = month.atDay(1).dayOfWeek.value - 1 // number of days between monday and 1
        val numberOfDays = month.lengthOfMonth() // days in month
        for (day in 1..numberOfDays) {
            // foreach day in month
            val index = day + padding - 1
            val column = index / 7 + 1
            // add missing row if needed
            if (column >= calendar.rowCount) calendar.rowConstraints += RowConstraints().apply {
                vgrow = Priority.SOMETIMES
            }

            // add and save label
            calendar.add(CenteredLabel(day.toString()).apply {
                onMouseClicked = EventHandler { selectDay(day) }
                days[day - 1] = this
            }, index % 7, column)

        }

        // remove extra rows
        while (calendar.rowCount > (numberOfDays + padding - 1) / 7 + 2) {
            calendar.rowConstraints.removeAt(calendar.rowCount - 1)
        }
    }

    /**
     * select a specific day
     */
    private fun selectDay(day: Int) = AppController.runBackground { model: Model.Editor ->
        model.day = day
    }

    /**
     * unselect if existing
     */
    private fun unselectDay() {
        if (selected != -1) days[selected]?.border = null
        selected = -1
    }
}