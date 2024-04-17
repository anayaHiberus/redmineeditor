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
        out.println("BEGIN:VCALENDAR")
        out.println("VERSION:2.0")
        out.println("PRODID:-//$name//RedmineEditor")
        out.println("NAME:Calendar $name")
        fullDays.forEach { fullDay ->
            out.println("BEGIN:VEVENT")
            out.println("DTSTAMP:" + ISO_COMPACT.format(LocalDateTime.now()))
            out.println("UID:" + fullDay.id)
            out.println("DTSTART;VALUE=DATE:" + fullDay.fromAsISO)
            out.println("DTEND;VALUE=DATE:" + fullDay.toAsISO)
            out.println("SUMMARY:" + fullDay.summary.replace("\n", "\\n"))
//            out.println("DESCRIPTION:description_description")
            if (fullDay.outOfOffice) {
                // this sets the Out-of-Office flag, but only works for outlook. There is no standard yet.
                out.println("X-MICROSOFT-CDO-BUSYSTATUS:OOF")
            }
            out.println("END:VEVENT")
        }
        out.println("END:VCALENDAR")
    }
}

/** A FullDay for ICS calendar. */
class FullDay(val from: LocalDate, val to: LocalDate, val summary: String, val outOfOffice: Boolean = false) {
    val fromAsISO get() = BASIC_ISO_DATE.format(from)
    val toAsISO get() = BASIC_ISO_DATE.format(to)
    val id get() = UUID.nameUUIDFromBytes("$from$to$summary".toByteArray())
}

private val ISO_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")