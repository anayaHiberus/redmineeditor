package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.DatePicker
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.TextArea
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import javafx.util.StringConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

/**
 * Show the calendar statistics dialog
 */
fun ShowCalendarStatisticsDialog() {
    Stage().apply {
        title = "Calendar Statistics"
        initModality(Modality.APPLICATION_MODAL)
        scene = Scene(FXMLLoader(ResourceLayout("calendar_statistics")).load())
        scene.stylize()
        centerInMouseScreen()
    }.showAndWait()
}


/**
 * The calendar statistics dialog controller
 */
class CalendarStatisticsController {

    /* ------------------------- elements ------------------------- */

    @FXML
    lateinit var logs: TextArea

    @FXML
    lateinit var toDate: DatePicker

    @FXML
    lateinit var fromDate: DatePicker

    @FXML
    lateinit var presets: MenuButton

    @FXML
    fun initialize() {
        // set date format
        object : StringConverter<LocalDate?>() {
            override fun toString(localDate: LocalDate?) = localDate?.let { DAY_FORMATTER.format(it) } ?: ""
            override fun fromString(dateString: String) = runCatching { LocalDate.parse(dateString, DAY_FORMATTER) }.getOrNull()
        }.let {
            fromDate.converter = it
            toDate.converter = it
        }

        // fill presets
        presets.items.addAll(
            MenuItem("current day").apply { setOnAction { fromDate.value = now(); toDate.value = now() } },
            MenuItem("current week").apply { setOnAction { fromDate.value = now().with(WeekFields.ISO.dayOfWeek(), 1); toDate.value = now().with(WeekFields.ISO.dayOfWeek(), 7) } },
            MenuItem("current month").apply { setOnAction { fromDate.value = now().withDayOfMonth(1); toDate.value = now().atEndOfMonth() } },
            MenuItem("last month").apply { setOnAction { fromDate.value = now().minusMonths(1).withDayOfMonth(1); toDate.value = now().minusMonths(1).atEndOfMonth() } },
            MenuItem("current year").apply { setOnAction { fromDate.value = now().withDayOfYear(1); toDate.value = now().withDayOfYear(now().lengthOfYear()) } }
                // apply this preset
                .apply { onAction.handle(null) },
        )
    }

    @FXML
    fun calculate() {
        // get
        val from = fromDate.value
        val to = toDate.value
        if (from > to) {
            // interval is inverted, switch values
            toDate.value = from
            fromDate.value = to
        }

        // set
        logs.text = calculateStatistics(from, to)
    }

    @FXML
    fun close() = logs.scene.window.run { fireEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)) }
}

/* ------------------------- Command line ------------------------- */

class CalendarStatisticsCommand : Command {
    override val name = "Command line variant of the CalendarStatistics tool"
    override val argument = "-calStats"
    override val parameters = "[--from=1997-01-01] [--to=1997-12-31]"
    override val help = listOf(
        "--from=1997-01-01, start of the range, inclusive. Must be a valid ISO java format. If not specified, today will be used.",
        "--to=1997-12-31, end of the range, inclusive. Must be a valid ISO java format. If not specified, today will be used.",
        "The output will be a 'table-like' string with the information",
    )

    override fun run(parameters: Application.Parameters) {
        listOf("from", "to").map { argument ->
            parameters.named[argument]?.let { parameter ->
                runCatching { LocalDate.parse(parameter) }.getOrElse {
                    println("Invalid ISO date for argument $argument=$parameter: ${it.message}")
                    return
                }
            } ?: now()
        }.let { (from, to) -> println(calculateStatistics(from, to)) }
    }

}

/* ------------------------- logic ------------------------- */

/** Calculate statistics from a time range. */
private fun calculateStatistics(from: LocalDate, to: LocalDate): String {
    // get
    if (from > to) {
        // interval is inverted, switch and retry
        return calculateStatistics(to, from)
    }
    data class HoursByScheduleDate(val schedule: Schedule, val date: LocalDate, val hours: Double)

    // load all calendars
    val expectedHours = getAllCalendars()
        .map { Schedule(it) }
        .filter { it.load() == null }
        .flatMap { schedule ->
            // and for each day in the range
            generateSequence(from) { date -> date.plusDays(1).takeIf { it <= to } }
                // generate an entry (the schedule at that date requires those hours)
                .map { date -> HoursByScheduleDate(schedule, date, schedule.expectedHours(date)) }
        }

    val matrix = mutableListOf<List<String>?>()

    // headers
    matrix.add(listOf("Element", "\\ Calendar") + expectedHours.map { it.schedule.calendar ?: "error" }.distinct())
    matrix.add(null)

    // add statistics about some entries
    fun calculate(label: String, groupedEntries: List<HoursByScheduleDate>) {
        // total days
        matrix.add(listOf(label, "Days") + groupedEntries.groupBy { it.schedule }.map { (_, entries) ->
            entries.size.toString()
        })
        // statistic about days count
        listOf<Pair<String, (HoursByScheduleDate) -> Boolean>>(
            "Non 0h days" to { it.hours != 0.0 },
            "L-V 0h days" to { it.hours == 0.0 && it.date.dayOfWeek <= DayOfWeek.FRIDAY },
            "S-D 0h days" to { it.hours == 0.0 && DayOfWeek.SATURDAY <= it.date.dayOfWeek },
        ).forEach { (label, filter) ->
            matrix.add(listOf("", label) + groupedEntries.groupBy { it.schedule }.map { (_, entries) ->
                entries.count(filter).toString()
            })
        }
        // hours
        matrix.add(listOf("", "Expected hours") + groupedEntries.groupBy { it.schedule }.map { (_, entries) ->
            entries.sumOf { it.hours }.toString()
        })
        matrix.add(null)
    }

    // total info (with multiple years)
    if (expectedHours.distinctBy { it.date.year }.size > 1)
        calculate("Total", expectedHours)

    // specific year info (with multiple months)
    if (expectedHours.distinctBy { it.date.yearMonth }.size > 1)
        expectedHours.groupBy { it.date.year }.forEach { (year, groupedEntries) ->
            calculate(year.toString(), groupedEntries)
        }

    // specific month info (with multiple days)
    if (expectedHours.distinctBy { it.date }.size > 1)
        expectedHours.groupBy { it.date.yearMonth }.forEach { (yearMonth, groupedEntries) ->
            calculate(yearMonth.toString(), groupedEntries)
        }

    // specific day info
    expectedHours.groupBy { it.date }.forEach { (date, groupedEntries) ->
        matrix.add(listOf(date.format(DAY_FORMATTER), "Expected hours") + groupedEntries.groupBy { it.schedule }.map { (_, entries) ->
            entries.sumOf { it.hours }.toString() // this should be just one entry anyway
        })
    }

    // format table
    val columnsSize = matrix[0]!!.indices.map { i -> matrix.maxOf { it?.getOrNull(i)?.length ?: 0 } }
    return "Interval: [${from.format(DAY_FORMATTER)}, ${to.format(DAY_FORMATTER)}]\n\n" + matrix.joinToString("\n") {
        it?.mapIndexed { i, value -> value.padStart(columnsSize[i]) }?.joinToString("|")
            ?: "-".repeat(columnsSize.sum() + columnsSize.size - 1)
    }
}

/** How to format a single day. */
private val DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEEE)")