package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.TimeEntry
import javafx.scene.paint.Color
import java.time.LocalDate

/**
 * the color of an issue, null if default
 */
val Issue.color
    get() = if (project == "Hiberus - Vacaciones") Color.CORNFLOWERBLUE else null

/**
 * The default issue color
 */
val DEFAULT_ISSUE_COLOR = Color.LIGHTGREEN

/**
 * Calculates the color based on the day, and hours
 *
 * @param expected expected hours that day, probably from [expectedHours]
 * @param spent    spent hours that day
 * @param day      the day
 * @param goodColor the color to return for a 'good' day (has a default value)
 * @return the color of that day (null for no color)
 */
fun getColor(expected: Double, spent: Double, day: LocalDate, goodColor: Color? = DEFAULT_ISSUE_COLOR) = when {
    // something to spend, and correctly spent, GOOD!
    expected != 0.0 && expected == spent -> goodColor
    // nothing to spend and nothing spent, HOLIDAY!
    expected == 0.0 && spent == 0.0 -> Color.LIGHTGREY
    // spent greater than expected, ERROR!
    spent > expected -> Color.INDIANRED
    // today, but still not all, WARNING!
    day == LocalDate.now() -> Color.ORANGE
    // past day and not all, ERROR!
    day.isBefore(LocalDate.now()) -> Color.RED
    // future day, but something spent, IN PROGRESS
    spent > 0 -> goodColor?.desaturate()
    // future day, NOTHING!
    else -> null // (null = no color)
}
