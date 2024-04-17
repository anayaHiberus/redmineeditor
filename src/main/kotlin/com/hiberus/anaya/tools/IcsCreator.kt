package com.hiberus.anaya.tools

import com.hiberus.anaya.icsapi.FullDay
import com.hiberus.anaya.icsapi.ICS
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.Schedule
import com.hiberus.anaya.redmineeditor.utils.errorln
import javafx.application.Application
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.temporal.TemporalAdjusters.firstDayOfYear
import java.time.temporal.TemporalAdjusters.lastDayOfYear

/** Command to create ICS files from a calendar file. */
class IcsCreator : Command {
    override val name = "ICS creator"
    override val argument = "-ics"
    override val parameters = "[--from=1997-01-01] [--to=1997-12-31] [--calendar=zaragoza] [--file=zaragoza.ics]"
    override val help = listOf(
        "--from=1997-01-01, start of the range, inclusive. Must be a valid ISO java format. If not specified, the start of the current year will be used.",
        "--to=1997-12-31, end of the range, inclusive. Must be a valid ISO java format. If not specified, the end of the current year will be used.",
        "--calendar=zaragoza, the name of the calendar to use. If not specified, the user-configured calendar will be used.",
        "--file=zaragoza.ics, the name of the output file. If not specified, it will have the same name as the calendar with the '.ics' extension added.",
    )

    override fun run(parameters: Application.Parameters) {
        // get inputs
        val (from, to) = listOf("from" to firstDayOfYear(), "to" to lastDayOfYear()).map { (argument, adjuster) ->
            parameters.named[argument]?.let { parameter ->
                runCatching { LocalDate.parse(parameter) }.getOrElse {
                    println("Invalid ISO date for argument $argument=$parameter: ${it.message}")
                    return
                }
            } ?: now().with(adjuster)
        }
        val calendar = parameters.named["calendar"] ?: AppSettings.SCHEDULE_FILE.value
        val schedule = Schedule(calendar)
        schedule.load()?.let {
            errorln(it)
            errorln("The calendar file $calendar had errors. Aborted.")
            return
        }
        val fileName = parameters.named["file"] ?: "${calendar}.ics"

        println("Generating ICS file ($fileName) for calendar $calendar between $from and $to")

        // generate ranges
        class Range(var from: LocalDate, var to: LocalDate)

        val ranges = mutableListOf<Range>()
        // for all days
        generateSequence(from) { date -> date.plusDays(1).takeIf { it <= to } }
            // except weekends
            .filter { it.dayOfWeek !in listOf(SATURDAY, SUNDAY) }
            // that don't require hours
            .filter { schedule.expectedHours(it) == 0.0 }
            .forEach { date ->

                // check previous
                ranges.lastOrNull()?.takeIf { it.to == date.minusDays(1) }?.let {
                    // extend previous range
                    it.to = date
                } ?: run {
                    // start a new range
                    ranges += Range(date, date)
                }
            }

        // generate events
        val ics = ICS(calendar)
        ranges.forEach { ics.addFullDaysRange(FullDay(it.from, it.to, "Holiday", outOfOffice = true)) }

        // create file
        ics.generateFile(fileName)
        print("ICS file generated with ${ranges.size} events.")
    }
}