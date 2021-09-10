package com.hiberus.anaya.hiberus;

import com.hiberus.anaya.utils.IsoDate;

import java.awt.*;
import java.util.Calendar;

public class Schedule {


    public static final Calendar NOW = Calendar.getInstance();

    static public double getExpectedHours(Calendar day) {
        int month = day.get(Calendar.MONTH);
        int weekday = day.get(Calendar.DAY_OF_WEEK);

        // TODO: add special days

        if (month == Calendar.JULY || month == Calendar.AUGUST) {
            // summer schedule
            return new double[]{-1, 0, 7, 7, 7, 7, 7, 0}[weekday];
        } else {
            // normal schedule
            return new double[]{-1, 0, 8.5, 8.5, 8.5, 8.5, 7, 0}[weekday];
        }

    }

    static public Color getColor(double expected, double spent, Calendar day) {
        if (expected == spent) {
            if (expected != 0)
                // Perfect day
                return Color.GREEN;
        } else {
            if (IsoDate.format(day).equals(IsoDate.format(NOW))) {
                // today still not all
                return Color.orange;
            } else if (day.compareTo(NOW) < 0) {
                // past day not all!
                return Color.red;
            }
        }

        // ignored
        return null;
    }

}
