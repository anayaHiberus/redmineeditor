package com.hiberus.anaya.redmineeditor.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

/**
 * Converts this hours amount into a string in the format (-)Xh Xm Xs
 * "0h", "1h", "1h" "15m", "30m", "40s", "1h" "60s", "10h" "30m", etc
 *
 * @return this number of hours formatted as string as described above
 */
fun Double.formatHours() =
    StringBuilder().apply {
        var remainder = this@formatHours

        if (remainder < 0) {
            // negative
            append("-")
            remainder = -remainder
        }

        // hours
        if (remainder >= 1) append(remainder.toInt()).append("h ")

        // minutes
        remainder = remainder % 1 * 60
        if (remainder >= 1) append(remainder.toInt()).append("m ")

        // seconds
        remainder = remainder % 1 * 60
        if (remainder >= 1) append(remainder.roundToInt()).append("s ")

        // check empty
        if (length <= 1) return "0h" // not even seconds (maybe negative) so that's 0h

    }.toString().trim() // remove last extra space

/** list of days of this month (as LocalDates) */
fun YearMonth.days() = (1..lengthOfMonth()).map { atDay(it) }

/** YearMoth of a full date */
val LocalDate.yearMonth get() = YearMonth.from(this)!!

/** Current date at the end of the month. */
fun LocalDate.atEndOfMonth() = withDayOfMonth(lengthOfMonth())

/** Current date at the end of the month. */
fun LocalDate.atEndOfYear() = withDayOfYear(lengthOfYear())

/** Current date with a specific day of week. */
fun LocalDate.withDayOfWeek(day: DayOfWeek) = with(WeekFields.ISO.dayOfWeek(), day.value.toLong())