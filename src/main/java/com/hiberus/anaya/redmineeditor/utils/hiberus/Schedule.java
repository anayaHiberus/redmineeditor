package com.hiberus.anaya.redmineeditor.utils.hiberus;

import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
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

        // special days
        if (SPECIAL.containsKey(day)) return SPECIAL.get(day);

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
            // something to spend, and correctly spent, GOOD!
            return Color.LIGHTGREEN;
        } else if (expected == 0 && spent == 0) {
            // nothing to spend and nothing spent, HOLIDAY!
            return Color.LIGHTGREY;
        } else if (spent > expected) {
            // spent greater than expected, ERROR!
            return Color.RED;
        } else if (day.equals(LocalDate.now())) {
            // today, but still not all, WARNING!
            return Color.ORANGE;
        } else if (day.isBefore(LocalDate.now())) {
            // past day and not all, ERROR!
            return Color.RED;
        } else {
            // future day, NOTHING YET!
            return null; // null = no color
        }
    }


    /* ------------------------- Special days ------------------------- */
    private static final Map<LocalDate, Double> SPECIAL = new HashMap<>();

    private static void special(int year, int month, int day, double hours) {
        // syntactic sugar for initialization below
        SPECIAL.put(LocalDate.of(year, month, day), hours);
    }

    private static void special(int year, int month, int day) {
        // syntactic sugar for initialization below (0 hours)
        SPECIAL.put(LocalDate.of(year, month, day), (double) 0);
    }

    static {
        // 2021
        // https://sommos.online/hiberus/calendario-laboral/calendario_hiberus_2021_zaragoza.pdf

        // Pilares
        special(2021, 10, 11, 7.33);
        special(2021, 10, 12);
        special(2021, 10, 13, 7.33);
        special(2021, 10, 14, 7.33);

        // Todos los santos
        special(2021, 11, 1);

        // Constitución
        special(2021, 12, 6);

        // Concepción
        special(2021, 12, 8);
    }

}
