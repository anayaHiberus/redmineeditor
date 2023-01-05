package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.model.AppSettings
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.paint.Color
import java.io.FileNotFoundException
import java.security.InvalidParameterException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * the expected hours you were supposed to spend this day
 */
val LocalDate.expectedHours
    // tries caching value, otherwise calculates it
    get() = CACHE.getOrPut(this) {
        // get first match, 0 if none
        RULES.firstOrNull { it.matches(this) }?.hours ?: 0.0
    }

/**
 * the expected hours you were supposed to spend this month
 */
val YearMonth.expectedHours
    // just add all the hours of each day in month
    get() = days().sumOf { it.expectedHours }

/**
 * Calculates the color based on the day, and hours
 *
 * @param expected expected hours that day, probably from [expectedHours]
 * @param spent    spent hours that day
 * @param day      the day
 * @return the color of that day (null for no color)
 */
fun getColor(expected: Double, spent: Double, day: LocalDate) = when {
    // something to spend, and correctly spent, GOOD!
    expected != 0.0 && expected == spent -> Color.LIGHTGREEN
    // nothing to spend and nothing spent, HOLIDAY!
    expected == 0.0 && spent == 0.0 -> Color.LIGHTGREY
    // spent greater than expected, ERROR!
    spent > expected -> Color.INDIANRED
    // today, but still not all, WARNING!
    day == LocalDate.now() -> Color.ORANGE
    // past day and not all, ERROR!
    day.isBefore(LocalDate.now()) -> Color.RED
    // future day, but something spent, IN PROGRESS
    spent > 0 -> Color.LIGHTBLUE
    // future day, NOTHING!
    else -> null // (null = no color)
}

/* ------------------------- Special days ------------------------- */

/**
 * Load special days from the configuration file
 */
fun LoadSpecialDays() = runCatching {
    // clear first
    CACHE.clear()
    RULES.clear()
    // get file
    (getSpecialDaysFile() ?: throw FileNotFoundException(getCalendarFile()))
        .readLines().asSequence()
        // remove comments
        .map { it.replace("#.*".toRegex(), "") }
        // skip empty
        .filter { it.isNotBlank() }
        // build valid to list
        .mapNotNull { line ->
            runCatching {
                Rule(line)
            }.onFailure {
                errorln("Invalid entry in hours file: \"${it.message}\"")
            }.getOrNull()
        }.toList()
        // and save (reversed)
        .reversed().toCollection(RULES)
}.onFailure {
    debugln(it)
}.isSuccess

/**
 * Opens the special days file in an external app
 */
fun OpenSpecialDaysFile() = (getSpecialDaysFile()?.openInApp() ?: false)
    .ifNotOK { Alert(Alert.AlertType.ERROR, "Can't open hours file").showAndWait() }

/**
 * Returns the special days file (if present)
 */
fun getSpecialDaysFile() = getRelativeFile(getCalendarFile())

/**
 * Replaces the special days file with [content] (asks first)
 * runs [onReplaced] if replaced
 */
fun replaceScheduleContent(content: String, onReplaced: () -> Unit) {
    // replace file content
    Alert(Alert.AlertType.CONFIRMATION).apply {
        title = "Replace file"
        contentText = "This will replace the content of the file ${getSpecialDaysFile()} with the remote version. Do you want to continue?"
        stylize()
        addButton(ButtonType.OK) {
            getSpecialDaysFile()?.writeText(content)
            onReplaced()
        }
    }.showAndWait()
}

/**
 * Returns the path of the currently selected calendar file
 */
fun getCalendarFile() = "conf/calendars/" + AppSettings.SCHEDULE_FILE.value.lowercase() + ".hours"

/* ------------------------- internal ------------------------- */

private val CACHE = mutableMapOf<LocalDate, Double>() // caching

private val RULES = mutableListOf<Rule>() // save elements

private class Rule(var line: String) {
    val year: Int?
    val month: Int?
    val day: Int?
    val week: DayOfWeek?
    val hours: Double
    val startDate: LocalDate?
    val endDate: LocalDate?

    init {

        // parse date ranges
        line.parseDate("<=").apply {
            line = first
            endDate = second
        }

        line.parseDate(">=").apply {
            line = first
            startDate = second
        }

        // split by comma
        val data = line.split(" ").map { it.trim() }
        // not enough
        if (data.size < 3) throw InvalidParameterException("not enough data: $line")

        // and other??
        if (data.size > 4) throw InvalidParameterException("more than enough data: $line")

        // year, month, day and hours
        year = data[0].toNullableInt()
        month = data[1].toNullableInt()
        val (d, w) = listOf("m", "t", "w", "th", "f", "sa", "su").indexOf(data[2]).takeIf { it != -1 }
            ?.let { null to DayOfWeek.of(it + 1) } // day of week
            ?: (data[2].toNullableInt() to null)  // normal day
        day = d
        week = w
        hours = data.getOrNull(3)?.toDouble() ?: 0.0

    }

    /**
     * '*' -> null else toInt
     */
    fun String.toNullableInt() = if (this == "*") null else toInt()

    /**
     * Parses a tail dateformat from a separator (null if no separator is found), returns a pair
     * "abc | 2020 02 02".parseDate("|") == ("abc", Date(2020,2,2))
     */
    fun String.parseDate(separator: String) = substringBeforeLast(separator).trimEnd() to
            if (contains(separator)) {
                LocalDate.parse(substringAfterLast(separator).trim(), DateTimeFormatter.ofPattern("yyyy M d"))
            } else {
                null
            }

    /**
     * true if the given date matches this rule
     */
    fun matches(date: LocalDate) =
        (startDate?.let { date >= it } ?: true)
                && (endDate?.let { date <= it } ?: true)
                && (year?.let { date.year == it } ?: true)
                && (month?.let { date.monthValue == it } ?: true)
                && (day?.let { date.dayOfMonth == it } ?: true)
                && (week?.let { date.dayOfWeek == it } ?: true)

}