package com.hiberus.anaya.redmineeditor.utils

import java.time.LocalDate
import java.time.temporal.ChronoField

class LocalDateRange(val fromDate: LocalDate, val toDate: LocalDate)

/**
 * Returns all days between the two local dates (inclusive).
 * If null, or start > end, returns empty list
 */
val LocalDateRange?.days: List<LocalDate>
    get() {
        // check invalid
        if (this == null) return emptyList()
        if (fromDate > toDate) return emptyList()
        // generate
        return mutableListOf(fromDate).apply {
            while (last() < toDate) add(last().plusDays(1))
        }
    }

/* ------------------------- Custom date format ------------------------- */

val WEEK_DAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")

val CUSTOM_DATE_FORMAT_EXPLANATION = "Format: three elements separated by slash: where each element can be a number or an offset ('1', '+1' or '-1') with an optional extra day of week element $WEEK_DAYS. The day can also be 'end' meaning 'end of month' (+0/+0/end). Examples: '2020/12/31'='last day of 2020', '+0/12/31'='last day of current year', '-1/+0/end'='last day of current month from last year', '+0/+0/+0/mon'='monday of current week'"

const val TODAY_DEFAULT = "Today (+0/+0/+0) by default"

/** Parses a date in the custom format. */
fun String.parseCustomDateFormat(): LocalDate? {
    var date = LocalDate.now()
    val parts = split("/")
    if (parts.size < 3 || parts.size > 4) {
        throw RuntimeException("The date must consist on 3 (or 4) elements separated by '/', found ${parts.size}")
    }
    // year
    parts[0].let {
        date = when {
            it.startsWith("+") -> date.plusYears(it.removePrefix("+").toLongOrNull() ?: throw RuntimeException("The year is not a valid positive offset"))
            it.startsWith("-") -> date.minusYears(it.removePrefix("-").toLongOrNull() ?: throw RuntimeException("The year is not a valid negative offset"))
            else -> date.withYear(it.toIntOrNull() ?: throw RuntimeException("The year is not a valid number"))
        }
    }
    // month
    parts[1].let {
        date = when {
            it.startsWith("+") -> date.plusMonths(it.removePrefix("+").toLongOrNull() ?: throw RuntimeException("The month is not a valid positive offset"))
            it.startsWith("-") -> date.minusMonths(it.removePrefix("-").toLongOrNull() ?: throw RuntimeException("The month is not a valid negative offset"))
            else -> date.withMonth(it.toIntOrNull() ?: throw RuntimeException("The month is not a valid number"))
        }
    }
    // day
    parts[2].let {
        date = when {
            it == "end" -> date.atEndOfMonth()
            it.startsWith("+") -> date.plusDays(it.removePrefix("+").toLongOrNull() ?: throw RuntimeException("The day is not a valid positive offset"))
            it.startsWith("-") -> date.minusDays(it.removePrefix("-").toLongOrNull() ?: throw RuntimeException("The day is not a valid negative offset"))
            else -> date.withDayOfMonth(it.toIntOrNull() ?: throw RuntimeException("The month is not a valid number"))
        }
    }
    // week
    parts.getOrNull(3)?.lowercase()?.let { week ->
        date = date.with(ChronoField.DAY_OF_WEEK, WEEK_DAYS.indexOf(week).toLong().takeIf { it != -1L } ?: throw RuntimeException("The week '$week' is not one of: $WEEK_DAYS"))
    }

    return date
}