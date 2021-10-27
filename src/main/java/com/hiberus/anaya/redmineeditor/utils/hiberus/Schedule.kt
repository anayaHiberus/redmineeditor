package com.hiberus.anaya.redmineeditor.utils.hiberus

import com.hiberus.anaya.redmineeditor.utils.findFile
import javafx.scene.paint.Color
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/**
 * the expected hours you were supposed to spend this day
 */
val LocalDate.expectedHours
    // if special day, return it
    get() = SPECIAL.getOrElse(this) {
        // otherwise, get default
        when (month) {
            // summer schedule
            Month.JULY, Month.AUGUST -> doubleArrayOf(7.0, 7.0, 7.0, 7.0, 7.0, 0.0, 0.0)
            // normal schedule
            else -> doubleArrayOf(8.5, 8.5, 8.5, 8.5, 7.0, 0.0, 0.0)
        }[dayOfWeek.value - 1] // 0 = monday, 6 = sunday
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
    // future day, NOTHING! (null = no color)
    else -> null
}

/* ------------------------- Special days ------------------------- */

/**
 * true iff the special days were loaded
 * TODO: remove this
 */
var SpecialDaysLoaded = false

/**
 * Load special days from the configuration file
 */
fun LoadSpecialDays() = runCatching {
    SPECIAL.clear()
    SpecialDaysLoaded = false
    findFile("conf/special_days.conf").readLines().asSequence()
        // remove comments
        .map { it.replace("#.*".toRegex(), "") }
        // skip empty
        .filter { it.isNotBlank() }
        // split by comma
        .map { line -> line.split(",").map { it.trim() } to line }
        // build valid to map
        .mapNotNull { (data, line) ->
            when {
                data.size < 3 -> {
                    // not enough
                    System.err.println("Invalid entry in schedule, not enough data: $line")
                    null
                }
                data.size == 3 -> {
                    // year, month and day. 0 hours
                    LocalDate.of(data[0].toInt(), data[1].toInt(), data[2].toInt()) to 0.0
                }
                else -> {
                    if (data.size > 4) {
                        // and other??
                        println("Invalid entry in schedule, more than enough data: $line")
                    }

                    // year, month, day and hours
                    LocalDate.of(data[0].toInt(), data[1].toInt(), data[2].toInt()) to data[3].toDouble()
                }
            }
        }.toMap()
        // and save
        .let {
            SPECIAL.putAll(it)
            SpecialDaysLoaded = true
        }
}.onFailure {
    println(it)
    System.err.println("Special days file error!")
}.isSuccess

/* ------------------------- internal ------------------------- */

private val SPECIAL = mutableMapOf<LocalDate, Double>()