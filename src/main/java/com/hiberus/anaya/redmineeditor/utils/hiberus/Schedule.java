package com.hiberus.anaya.redmineeditor.utils.hiberus;

import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.Month;

public class Schedule {

    static public double getExpectedHours(LocalDate day) {
        Month month = day.getMonth();
        int weekday = day.getDayOfWeek().getValue(); // 1 = monday, 7 = sunday

        // TODO: add special days

        if (month == Month.JULY || month == Month.AUGUST) {
            // summer schedule
            return new double[]{-1, 7, 7, 7, 7, 7, 0, 0}[weekday];
        } else {
            // normal schedule
            return new double[]{-1, 8.5, 8.5, 8.5, 8.5, 7, 0, 0}[weekday];
        }

    }

    static public Color getColor(double expected, double spent, LocalDate day) {
        if (expected == spent) {
            if (expected != 0)
                // something to spend, and correctly spent
                return Color.LIGHTGREEN;
            // nothing to spend and nothing spent -> ignore
        } else {
            if (day.equals(LocalDate.now())) {
                // today but still not all
                return Color.ORANGE;
            } else if (day.isBefore(LocalDate.now())) {
                // past day and not all!
                return Color.RED;
            }
        }

        // ignored
        return null;
    }

}
