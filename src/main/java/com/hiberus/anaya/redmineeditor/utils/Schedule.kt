package com.hiberus.anaya.redmineeditor.utils

import javafx.scene.paint.Color
import java.security.InvalidParameterException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

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
    get() = (1..lengthOfMonth()).sumOf { atDay(it).expectedHours }

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
    CACHE.clear()
    RULES.clear()
    findFile("conf/hours.conf").readLines().asSequence()
        // remove comments
        .map { it.replace("#.*".toRegex(), "") }
        // skip empty
        .filter { it.isNotBlank() }
        // split by comma
        .map { line -> line.split(",").map { it.trim() } to line }
        // build valid to list
        .mapNotNull { (data, line) ->
            runCatching {
                when {
                    data.size < 3 -> {
                        // not enough
                        throw InvalidParameterException("not enough data: $line")
                    }
                    data.size == 3 -> {
                        // year, month and day. 0 hours
                        Rule(data[0], data[1], data[2], 0.0)
                    }
                    else -> {
                        if (data.size > 4) {
                            // and other??
                            throw InvalidParameterException("more than enough data: $line")
                        }

                        // year, month, day and hours
                        Rule(data[0], data[1], data[2], data[3].toDouble())
                    }
                }
            }.onFailure { System.err.println("Invalid entry in hours file: \"${it.message}\"") }.getOrNull()
        }.toList()
        // and save (reversed)
        .reversed().toCollection(RULES)
}.onFailure {
    println(it)
    System.err.println("Special days file error!")
}.isSuccess

/* ------------------------- internal ------------------------- */

private val CACHE = mutableMapOf<LocalDate, Double>() // caching

private val RULES = mutableListOf<Rule>() // save elements

private class Rule(year: String, month: String, day: String, val hours: Double) {
    private val year = if (year == "*") null else year.toInt()
    private val month = if (month == "*") null else month.toInt()
    private val day: Int?
    private val week: DayOfWeek?

    init {
        // converts the day into either a day or a day-of-week (THINK: allow both?)
        val (d, w) = listOf("m", "t", "w", "th", "f", "sa", "su").indexOf(day).takeIf { it != -1 }
            ?.let { null to DayOfWeek.of(it + 1) } // day of week
            ?: ((if (day == "*") null else day.toInt()) to null)  // normal day
        this.day = d
        this.week = w
    }

    /**
     * true if the given date matches this rule
     */
    fun matches(date: LocalDate) =
        (year?.let { date.year == it } ?: true)
                && (month?.let { date.monthValue == it } ?: true)
                && (day?.let { date.dayOfMonth == it } ?: true)
                && (week?.let { date.dayOfWeek == it } ?: true)

}