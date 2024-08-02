package com.hiberus.anaya.tools

import com.hiberus.anaya.icsapi.FullDay
import com.hiberus.anaya.icsapi.ICS
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.model.AppSettings
import com.hiberus.anaya.redmineeditor.utils.Calendar
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
        val calendarName = parameters.named["calendar"]?.titleCase() ?: AppSettings.SCHEDULE_FILE.value
        val calendar = Calendar(calendarName)
        val alsoCalendars = parameters.named["also"]
            ?.split(",")
            ?.map { it.trim().titleCase() }
            ?.filter { it != calendarName }
            ?.map { Calendar(it) }
            ?: emptyList()
        val fileName = parameters.named["file"] ?: "${calendarName}.ics"

        // load calendars
        (listOf(calendar) + alsoCalendars).forEach { cal ->
            cal.load()?.let {
                errorln(it)
                errorln("The calendar file ${cal.calendar} had errors. Aborted.")
                return
            }
        }

        println("Generating ICS file ($fileName) for calendar $calendarName checking also $alsoCalendars between $from and $to")

        // generate ranges
        class Range(var from: LocalDate, var to: LocalDate, var alsoIn: List<String>, var alsoNotIn: List<String>)

        val ranges = mutableListOf<Range>()
        // for all days
        generateSequence(from) { date -> date.plusDays(1).takeIf { it <= to } }
            // except weekends
            .filter { it.dayOfWeek !in listOf(SATURDAY, SUNDAY) }
            // that don't require hours
            .filter { calendar.expectedHours(it) == 0.0 }
            // associate with calendar that also have holidays (or not)
            .associateWith { date ->
                alsoCalendars
                    .groupBy { it.expectedHours(date) == 0.0 }
                    .mapValues { (_, it) -> it.map { it.calendar ?: "?" } }
            }
            .forEach { (date, also) ->

                // check previous
                ranges.lastOrNull()?.takeIf { it.to == date.minusDays(1) }?.let {
                    // extend previous range
                    it.to = date
                } ?: run {
                    // start a new range
                    ranges += Range(date, date, also[true] ?: emptyList(), also[false] ?: emptyList())
                }
            }

        // generate events
        val ics = ICS(calendarName)
        ranges.forEach {
            ics.addFullDaysRange(
                FullDay(
                    from = it.from,
                    to = it.to,
                    summary = "Holiday ($calendarName)",
                    description = buildString {
                        if (it.alsoIn.isNotEmpty()) {
                            appendLine("Also holiday in ${it.alsoIn.joinToString(", ")}")
                            appendLine()
                        }
                        if (it.alsoNotIn.isNotEmpty()) {
                            appendLine("Also, not a holiday in ${it.alsoNotIn.joinToString(", ")}")
                            appendLine()
                        }
                        append("Generated with RedmineEditor (https://github.com/anayaHiberus/redmineeditor/releases/tag/ics)")
                    },
                    outOfOffice = true,
                )
            )
        }

        // create file
        ics.generateFile(fileName)
        print("ICS file generated with ${ranges.size} events.")
    }
}

/** Converts a string into title case: "Kotlin is tHe BEST" -> "Kotlin Is The Best". */
fun String.titleCase(delimiter: String = " ") =
    split(delimiter).joinToString(delimiter) { word ->
        word.lowercase().replaceFirstChar { it.titlecaseChar() }
    }