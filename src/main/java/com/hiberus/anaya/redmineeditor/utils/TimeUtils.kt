package com.hiberus.anaya.redmineeditor.utils

import kotlin.math.roundToInt

/**
 * Time utilities
 */

object TimeUtils {

    /**
     * Converts a hours number into a string in the format (-)Xh Xm Xs
     * 0h, 1h, 1h 15m, 30m, 40s, 1h 60s, 10h 30m, etc
     *
     * @param hours number of hours
     * @return that number of hours formatted as string as described above
     */
    @JvmStatic
    fun formatHours(hours: Double): String =
        StringBuilder().apply {
            var rest = hours

            if (rest < 0) {
                // negative
                append("-")
                rest = -rest
            }

            // hours
            if (rest >= 1) append(rest.toInt()).append("h ")

            // minutes
            rest = rest % 1 * 60
            if (rest >= 1) append(rest.toInt()).append("m ")

            // seconds
            rest = rest % 1 * 60
            if (rest >= 1) append(rest.roundToInt()).append("s ")

            // check empty
            if (length <= 1) return "0h" // not even seconds (maybe negative) so that's 0h

        }.toString().trim() // remove last extra space
}