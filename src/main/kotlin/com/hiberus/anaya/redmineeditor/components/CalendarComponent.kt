package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.dialogs.MyException
import com.hiberus.anaya.redmineeditor.model.*
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.*

/** A calendar view with colored days */
internal class CalendarComponent {

    /* ------------------------- properties ------------------------- */

    /** the currently displayed labels */
    private val days = arrayOfNulls<Label>(31)

    /** the selected day index from [days] */
    private var selected = -1

    /* ------------------------- views ------------------------- */

    lateinit var calendarLabel: Label // month/year label
    lateinit var calendar: GridPane // grid

    /* ------------------------- init ------------------------- */

    @FXML
    private fun initialize() {
        // create the header
        DayOfWeek.entries.forEach {
            // append each day
            calendar.add(
                CenteredLabel(
                    it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                ), it.value - 1, 0
            )
        }

        // on new month, draw grid
        AppController.onChanges(setOf(ChangeEvent.Month)) { model: Model ->
            drawGrid(model)
        }

        // when hours change, recolor day
        AppController.onChanges(setOf(ChangeEvent.DayHours)) { model: Model ->
            model.day?.let { colorDay(it, model) }
        }

        // when month or hours change, set label
        AppController.onChanges(setOf(ChangeEvent.Month, ChangeEvent.DayHours, ChangeEvent.MonthHours)) { model ->
            updateLabel(model)
        }

        // when month or month hours change, color days
        AppController.onChanges(setOf(ChangeEvent.Month, ChangeEvent.MonthHours)) { model: Model ->
            // color days
            colorDays(model)
        }

        // when day changes (or month), set selection
        AppController.onChanges(setOf(ChangeEvent.Day, ChangeEvent.Month)) { model: Model ->
            // unselect
            unselectDay()
            // select new (if there is a selection)
            model.day?.let {
                selected = it - 1
                days[selected]?.border =
                    Border(BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii(5.0), BorderWidths(1.0)))
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

    }

    /** color all days of current month */
    private fun colorDays(model: Model) =
        (1..model.month.lengthOfMonth())
            .forEach { colorDay(it, model) }

    /** color a single day of current month */
    private fun colorDay(day: Int, model: Model) {
        val date = model.month.atDay(day)
        val label = days[day - 1] ?: return

        // calculate spent
        val spent = model.getSpent(date)
        if (spent == null) {
            // not loaded yet
            label.backgroundColor = null
            return
        }

        // main color
        val mainColor = getColor(
            date.expectedHours,
            spent,
            date
        )

        // special color: average color of the used entries
        val specialColor = model.getLoadedEntriesFromDate(date)?.filter { it.spent > 0 }?.map { (it.issue.color ?: mainColor ?: Color.TRANSPARENT) to it.spent }?.average

        // intensive indicator: fixed color if the day is not extensive
        // TODO: move this hardcoded range somewhere configurable
        val intensiveColor = Colors.INTENSIVE.value.takeIf { 0.0 < date.expectedHours && date.expectedHours < 8.0 }

        // set
        label.indicatorColor(background = mainColor, topLeft = specialColor, verticalBars = intensiveColor)
    }

    /** Updates the month/year label and color */
    private fun updateLabel(model: Model) {
        val month = model.month
        // month info
        var label = DateTimeFormatterBuilder()
            .appendText(ChronoField.MONTH_OF_YEAR)
            .appendLiteral(", ")
            .appendText(ChronoField.YEAR)
            .toFormatter()
            .format(month)

        val spent = model.getSpent(month)
        if (spent != null) {
            // loaded, append spent/expected and set color
            val expected = month.expectedHours
            label += " (${spent.formatHours()}/${expected.formatHours()})"
            calendarLabel.backgroundColor = getColor(
                expected, spent,
                // last/today/first
                when {
                    month == LocalDate.now().yearMonth -> LocalDate.now()
                    month > LocalDate.now().yearMonth -> month.atDay(1)
                    else -> month.atEndOfMonth()
                }
            )
        } else {
            // not loaded, clear
            calendarLabel.backgroundColor = null
        }

        // set
        calendarLabel.text = label
    }

    /** recreates the days grid */
    private fun drawGrid(model: Model) {
        // clear
        calendar.children.removeAll(days.toSet())
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
                // today in bold
                if (LocalDate.now() == month.atDay(day)) style = "-fx-font-weight: bold"
                days[day - 1] = this
            }, index % 7, column)

        }

        // remove extra rows
        while (calendar.rowCount > (numberOfDays + padding - 1) / 7 + 2) {
            calendar.rowConstraints.removeAt(calendar.rowCount - 1)
        }
    }

    /** select a specific day */
    private fun selectDay(day: Int) = AppController.runBackground { model ->
        model.day = day
    }

    /** unselect if existing */
    private fun unselectDay() {
        if (selected != -1) days[selected]?.border = null
        selected = -1
    }
}
