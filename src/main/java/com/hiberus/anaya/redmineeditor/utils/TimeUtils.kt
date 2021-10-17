package com.hiberus.anaya.redmineeditor.utils

import kotlin.math.roundToInt

/**
 * Converts this hours amount into a string in the format (-)Xh Xm Xs
 * 0h, 1h, 1h 15m, 30m, 40s, 1h 60s, 10h 30m, etc
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