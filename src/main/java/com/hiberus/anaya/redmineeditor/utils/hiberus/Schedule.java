package com.hiberus.anaya.redmineeditor.utils.hiberus;

import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.stream.IntStream;

/**
 * Logic regarding computed hours
 */
public class Schedule {

    /**
     * Calculates the hours you were supposed to spend on a day
     *
     * @param day which day to check
     * @return the hours you need to spend that day
     */
    static public double getExpectedHours(LocalDate day) {
        Month month = day.getMonth();
        int weekday = day.getDayOfWeek().getValue(); // 1 = monday, 7 = sunday

        // TODO: add special days like holidays

        if (month == Month.JULY || month == Month.AUGUST) {
            // summer schedule
            return new double[]{-1, 7, 7, 7, 7, 7, 0, 0}[weekday];
        } else {
            // normal schedule
            return new double[]{-1, 8.5, 8.5, 8.5, 8.5, 7, 0, 0}[weekday];
        }
    }

    /**
     * Calculates the hours you were supposed to spend on a month
     *
     * @param month which month to check
     * @return the hours you need to spend that month
     */
    public static double getExpectedHours(YearMonth month) {
        return IntStream
                // for each day of the month
                .rangeClosed(1, month.lengthOfMonth())
                // get the expected hours that day
                .mapToDouble(day -> getExpectedHours(month.atDay(day)))
                // and sum
                .sum();
    }

    /**
     * Calculates the color based on the day, and hours
     *
     * @param expected expected hours that day, probably from {@link Schedule#getExpectedHours(LocalDate)}
     * @param spent    spent hours that day
     * @param day      the day
     * @return the color of that day (null for no color)
     */
    static public Color getColor(double expected, double spent, LocalDate day) {
        if (expected != 0 && expected == spent) {
            return Color.LIGHTGREEN;
        } else if (expected == 0 && spent == 0) {
            // nothing to spend and nothing spent -> ignore
        } else if (spent > expected) {
            // spent greater than expected, ERROR!
            return Color.RED;
        } else if (day.equals(LocalDate.now())) {
            // today, but still not all, WARNING!
            return Color.ORANGE;
        } else if (day.isBefore(LocalDate.now())) {
            // past day and not all, ERROR!
            return Color.RED;
        }
        // future day -> ignore

        // ignored, null (no color)
        return null;
    }

}
