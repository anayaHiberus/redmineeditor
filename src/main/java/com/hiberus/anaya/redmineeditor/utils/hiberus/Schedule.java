package com.hiberus.anaya.redmineeditor.utils.hiberus;

import javafx.scene.paint.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    static {
        // Load special days from hardcoded file
        String filename = "/home/anaya/abel/personal/proyectos/redmine/SpecialDays.csv";

        try (Stream<String> lines = Files.lines(Paths.get(filename))) {
            lines
                    .map(line -> line.replaceAll("#.*", "")) // remove comments
                    .filter(s -> !s.isBlank()) // skip empty
                    .forEach(line -> {
                        // parse data
                        List<String> data = Arrays.stream(line.split(",")).map(String::strip).collect(Collectors.toList());
                        if (data.size() < 3) {
                            // not enough
                            System.err.println("[ERROR] Not enough data in " + filename + "> " + line);
                        } else if (data.size() == 3) {
                            // year, month and day. 0 hours
                            SPECIAL.put(LocalDate.of(
                                    Integer.parseInt(data.get(0)),
                                    Integer.parseInt(data.get(1)),
                                    Integer.parseInt(data.get(2))
                            ), (double) 0);
                        } else {
                            // year, month, day and hours
                            SPECIAL.put(LocalDate.of(
                                    Integer.parseInt(data.get(0)),
                                    Integer.parseInt(data.get(1)),
                                    Integer.parseInt(data.get(2))
                            ), Double.parseDouble(data.get(3)));

                            if (data.size() > 4) {
                                // and other??
                                System.out.println("[Warning] more than necessary data in " + filename + "> " + line);
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Special days file error!");
        }
    }

}
