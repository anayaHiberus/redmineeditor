package com.hiberus.anaya.redmineeditor.utils;

/**
 * Time utilities
 */
public class TimeUtils {

    /**
     * Converts an hours number into a string in the format (-)Xh Xm Xs
     * 0h, 1h, 1h 15m, 30m, 40s, 1h 60s, 10h 30m, etc
     *
     * @param hours number of hours
     * @return that number of hours formatted as string as described above
     */
    public static String formatHours(double hours) {
        StringBuilder result = new StringBuilder();
        if (hours < 0) {
            // negative
            result.append("-");
            hours = -hours;
        }

        // hours
        if (hours >= 1) result.append((int) hours).append("h ");

        // minutes
        hours = (hours % 1) * 60;
        if (hours >= 1) result.append((int) hours).append("m ");

        // seconds
        hours = (hours % 1) * 60;
        if (hours >= 1) result.append(Math.round(hours)).append("s ");

        return result.length() <= 1 ? "0h" // not even seconds (maybe negative) so that's 0h
                : result.toString().strip(); // remove last extra space
    }
}
