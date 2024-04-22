package com.hiberus.anaya.icsapi

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.util.*

/** Manual implementation of an ICS creator tool. */
class ICS(val name: String) {

    private val fullDays = mutableListOf<FullDay>()

    /** Adds a full day range. Both ends are inclusive. */
    fun addFullDaysRange(fullDay: FullDay) = fullDays.add(fullDay)

    /** Generates an ICS file. */
    fun generateFile(fileName: String) = File(fileName).printWriter().use { out ->
        fun write(line: String) = out.print(line + "\r\n")

        write("BEGIN:VCALENDAR")
        write("VERSION:2.0")
        write("PRODID:-//$name//RedmineEditor")
        write("NAME:Calendar $name")
        fullDays.forEach { fullDay ->
            write("BEGIN:VEVENT")
            write("DTSTAMP:" + ISO_COMPACT.format(LocalDateTime.now()))
            write("UID:" + fullDay.id)
            write("DTSTART;VALUE=DATE:" + fullDay.fromAsISO)
            write("DTEND;VALUE=DATE:" + fullDay.toAsISO)
            write("SUMMARY:" + fullDay.summary.encoded)
            write("DESCRIPTION:" + fullDay.description.encoded)
            if (fullDay.outOfOffice) {
                // this sets the Out-of-Office flag, but only works for outlook. There is no standard yet.
                write("X-MICROSOFT-CDO-BUSYSTATUS:OOF")
            }
            write("END:VEVENT")
        }
        write("END:VCALENDAR")
    }
}

/** A FullDay for ICS calendar. */
class FullDay(
    val from: LocalDate,
    val to: LocalDate,
    val summary: String,
    val description: String = "",
    val outOfOffice: Boolean = false
) {
    val fromAsISO get() = BASIC_ISO_DATE.format(from)
    val toAsISO get() = BASIC_ISO_DATE.format(to)
    val id get() = UUID.nameUUIDFromBytes("$from$to$summary".toByteArray())
}

private val ISO_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

// this needs a proper encoding
private val String.encoded get() = replace("\n", "\\n")