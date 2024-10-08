package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineeditor.model.AppSettings
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import java.io.FileNotFoundException
import java.security.InvalidParameterException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Manages a Calendar. */
class Calendar(val calendar: String? = null) {
    private val cache = mutableMapOf<LocalDate, Double>() // caching
    private val rules = mutableListOf<Rule>() // save elements

    /** Loads the file. Returns errors, if any */
    fun load() = runCatching {
        // clear first
        cache.clear()
        rules.clear()

        // get file
        val (rules, errors) = (getCalendarFile(calendar) ?: throw FileNotFoundException("Calendar ${getCalendarFilePath(calendar)} not found, maybe it was renamed?\nPlease choose another one from the settings."))
            // parse lines
            .readLines().asSequence()
            .let { parseCalendar(it) }

        // and save (reversed)
        rules.reversed().toCollection(this.rules)

        errors
    }.getOrElse { exception ->
        exception.message?.also { debugln(it) }
    }

    /** Returns the expected hours you were supposed to spend a specific date. */
    fun expectedHours(date: LocalDate) = cache.getOrPut(date) {
        // get first match, 0 if none
        rules.firstOrNull { it.matches(date) }?.hours ?: 0.0
    }

}


/* ------------------------- current calendar ------------------------- */

private val SELECTED_CALENDAR = Calendar()

/** Load calendar from the configuration file */
fun LoadCalendar() = SELECTED_CALENDAR.load()

/** the expected hours you were supposed to spend this day */
val LocalDate.expectedHours
    // tries caching value, otherwise calculates it
    get() = SELECTED_CALENDAR.expectedHours(this)

/** the expected hours you were supposed to spend this month */
val YearMonth.expectedHours
    // just add all the hours of each day in month
    get() = days().sumOf { it.expectedHours }


/* ------------------------- calendar file ------------------------- */


/** Opens the calendar file in an external app */
fun OpenCalendarFile() = (getCalendarFile()?.openInApp() ?: false)
    .ifNotOK { Alert(Alert.AlertType.ERROR, "Can't open hours file").showAndWait() }

/** Returns the calendar file from its name (current if unspecified) (if present) */
private fun getCalendarFile(calendar: String? = null) = getRelativeFile(getCalendarFilePath(calendar))

/**
 * Replaces the calendar file (current if unspecified) with [content] (asks first)
 * runs [onReplaced] if replaced
 */
fun replaceCalendarContent(content: String, calendar: String? = null, onReplaced: () -> Unit) {

    // replace file content
    Alert(Alert.AlertType.CONFIRMATION).apply {
        title = "Replace file"
        contentText = "This will replace the content of the file ${getCalendarFile(calendar)} with the remote version. Do you want to continue?"
        stylize()
        addButton(ButtonType.OK) {
            getCalendarFile(calendar)?.writeText(content)
            onReplaced()
        }
    }.showAndWait()
}

/** Returns the path of a calendar file (current by default) */
fun getCalendarFilePath(calendar: String? = null) = CALENDARS_FOLDER + (calendar ?: AppSettings.SCHEDULE_FILE.value).lowercase() + CALENDARS_EXTENSION

/** Returns all calendar paths. */
fun getAllCalendars() = getAllFiles(CALENDARS_FOLDER) { _, name -> name.endsWith(CALENDARS_EXTENSION) }.map { it.extractFileName() }.sorted()

/** Returns true iff there are rules in [lines] not present in the current rules */
fun areNewerRules(lines: Sequence<String>, calendar: String? = null) = parseCalendar(lines).first.toMutableList().apply { removeAll(parseCalendar(getRelativeFile(getCalendarFilePath(calendar))?.readLines()?.asSequence() ?: throw Exception("Invalid calendar file")).first) }.isNotEmpty()


/* ------------------------- internal ------------------------- */

private const val CALENDARS_FOLDER = "conf/calendars/"

private const val CALENDARS_EXTENSION = ".hours"

/** Reads a calendar file and converts its content to a list of rules, returns also a 'valid' boolean */
private fun parseCalendar(lines: Sequence<String>) = lines
    // remove comments
    .map { it.replace("#.*".toRegex(), "") }
    // skip empty
    .filter { it.isNotBlank() }
    // build valid to list
    .map { line ->
        runCatching {
            Rule(line) to null
        }.getOrElse { exception ->
            null to exception.message?.also { errorln("Invalid line in hours file: $it") }
        }
    }.toList().let { list ->
        // zip rules and errors
        list.mapNotNull { it.first } to list.mapNotNull { it.second }.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

/** A calendar rule container. */
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
        if (data.size < 3) throw InvalidParameterException("not enough data: \"$line\"")

        // and other??
        if (data.size > 4) throw InvalidParameterException("more than enough data: \"$line\"")

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

    /** '*' -> null else toInt */
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

    /** true if the given date matches this rule */
    fun matches(date: LocalDate) =
        (startDate?.let { date >= it } ?: true)
                && (endDate?.let { date <= it } ?: true)
                && (year?.let { date.year == it } ?: true)
                && (month?.let { date.monthValue == it } ?: true)
                && (day?.let { date.dayOfMonth == it } ?: true)
                && (week?.let { date.dayOfWeek == it } ?: true)


    /* ------------------------- EQUALS & HASHCODE ------------------------- */

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Rule

        if (year != other.year) return false
        if (month != other.month) return false
        if (day != other.day) return false
        if (week != other.week) return false
        if (hours != other.hours) return false
        if (startDate != other.startDate) return false
        if (endDate != other.endDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = year ?: 0
        result = 31 * result + (month ?: 0)
        result = 31 * result + (day ?: 0)
        result = 31 * result + (week?.hashCode() ?: 0)
        result = 31 * result + hours.hashCode()
        result = 31 * result + (startDate?.hashCode() ?: 0)
        result = 31 * result + (endDate?.hashCode() ?: 0)
        return result
    }


}
