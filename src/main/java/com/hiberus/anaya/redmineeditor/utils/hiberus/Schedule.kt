package com.hiberus.anaya.redmineeditor.utils.hiberus

import javafx.scene.paint.Color
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/**
 * Logic regarding computed hours
 */
object Schedule {

    /**
     * Calculates the hours you were supposed to spend on a day
     *
     * @param day which day to check
     * @return the hours you need to spend that day
     */
    @JvmStatic
    fun getExpectedHours(day: LocalDate): Double =
        SPECIAL.getOrElse(day) { // if special day, just that one, else
            when (day.month) {
                Month.JULY, Month.AUGUST -> doubleArrayOf(7.0, 7.0, 7.0, 7.0, 7.0, 0.0, 0.0) // summer schedule
                else -> doubleArrayOf(8.5, 8.5, 8.5, 8.5, 7.0, 0.0, 0.0) // normal schedule
            }[day.dayOfWeek.value - 1] // 0 = monday, 6 = sunday
        }

    /**
     * Calculates the hours you were supposed to spend on a month
     *
     * @param month which month to check
     * @return the hours you need to spend that month
     */
    @JvmStatic
    fun getExpectedHours(month: YearMonth): Double =
        (1..month.lengthOfMonth()).sumOf { getExpectedHours(month.atDay(it)) }

    /**
     * Calculates the color based on the day, and hours
     *
     * @param expected expected hours that day, probably from [Schedule.getExpectedHours]
     * @param spent    spent hours that day
     * @param day      the day
     * @return the color of that day (null for no color)
     */
    @JvmStatic
    fun getColor(expected: Double, spent: Double, day: LocalDate): Color? = when {
        expected != 0.0 && expected == spent -> Color.LIGHTGREEN // something to spend, and correctly spent, GOOD!
        expected == 0.0 && spent == 0.0 -> Color.LIGHTGREY // nothing to spend and nothing spent, HOLIDAY!
        spent > expected -> Color.RED // spent greater than expected, ERROR!
        day == LocalDate.now() -> Color.ORANGE // today, but still not all, WARNING!
        day.isBefore(LocalDate.now()) -> Color.RED // past day and not all, ERROR!
        else -> null // future day, NOTHING YET! (null = no color)
    }

    /* ------------------------- Special days ------------------------- */
    private val SPECIAL: MutableMap<LocalDate, Double> = HashMap()

    init {
        // Load special days from hardcoded file
        val filename = "/home/anaya/abel/personal/proyectos/redmine/special_days.conf"
        try {
            Files.lines(Paths.get(filename)).use { lines ->
                lines.map { it.replace("#.*".toRegex(), "") } // remove comments
                    .filter(String::isNotBlank) // skip empty
                    .forEach { line: String ->
                        // parse data
                        val data = line.split(",").map { obj: String -> obj.trim() }
                        if (data.size < 3) {
                            // not enough
                            System.err.println("[ERROR] Not enough data in $filename> $line")
                        } else if (data.size == 3) {
                            // year, month and day. 0 hours
                            SPECIAL[LocalDate.of(data[0].toInt(), data[1].toInt(), data[2].toInt())] = 0.0
                        } else {
                            // year, month, day and hours
                            SPECIAL[LocalDate.of(data[0].toInt(), data[1].toInt(), data[2].toInt())] = data[3].toDouble()

                            if (data.size > 4) {
                                // and other??
                                println("[Warning] more than necessary data in $filename> $line")
                            }
                        }
                    }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            System.err.println("Special days file error!")
        }
    }
}