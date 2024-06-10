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

const val CUSTOM_DATE_FORMAT_EXPLANATION = "Format: three elements separated by slash: where each element can be a number or an offset ('1', '+1' or '-1') with an optional extra day of week (mon,tue,wed,thu,fri,sat,sun) element. The day can also be 'end' meaning 'end of month' (+0/+0/end). Examples: '2020/12/31'='last day of 2020', '+0/12/31'='last day of current year', '-1/+0/end'='last day of current month from last year', '+0/+0/+0/mon'='monday of current week'"

/** Parses a date in the custom format. */
fun String.parseCustomDateFormat(): LocalDate? {
    var date = LocalDate.now()
    val parts = split("/")
    if (parts.size < 3 || parts.size > 4) {
        println("Invalid date $this")
        return null
    }
    parts[0].let {
        date = when {
            it.startsWith("+") -> date.plusYears(it.removePrefix("+").toLong())
            it.startsWith("-") -> date.minusYears(it.removePrefix("-").toLong())
            else -> date.withYear(it.toInt())
        }
    }
    parts[1].let {
        date = when {
            it.startsWith("+") -> date.plusMonths(it.removePrefix("+").toLong())
            it.startsWith("-") -> date.minusMonths(it.removePrefix("-").toLong())
            else -> date.withMonth(it.toInt())
        }
    }
    parts[2].let {
        date = when {
            it == "end" -> date.atEndOfMonth()
            it.startsWith("+") -> date.plusDays(it.removePrefix("+").toLong())
            it.startsWith("-") -> date.minusDays(it.removePrefix("-").toLong())
            else -> date.withDayOfMonth(it.toInt())
        }
    }
    parts.getOrNull(3)?.let {
        date = date.with(ChronoField.DAY_OF_WEEK, listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").indexOf(it).toLong())
    }

    return date
}