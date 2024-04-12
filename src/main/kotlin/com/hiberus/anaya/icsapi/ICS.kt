package com.hiberus.anaya.icsapi

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import java.util.*

/** Manual implementation of an ICS creator tool. */
class ICS(val name: String) {

    private val days = mutableListOf<Triple<LocalDate, LocalDate, String>>()

    /** Adds a full day range. Both ends are inclusive. */
    fun addFullDaysRange(from: LocalDate, to: LocalDate, summary: String) = days.add(
        Triple(
            from,
            // range is exclusive
            to.plusDays(1),
            summary
        )
    )

    /** Generates an ICS file. */
    fun generateFile(fileName: String) = File(fileName).printWriter().use { out ->
        out.println("BEGIN:VCALENDAR")
        out.println("VERSION:2.0")
        out.println("PRODID:-//$name//RedmineEditor")
        out.println("NAME:Calendar $name")
        days.forEach { (from, to, summary) ->
            out.println("BEGIN:VEVENT")
            out.println("DTSTAMP:" + ISO_COMPACT.format(LocalDateTime.now()))
            out.println("UID:" + UUID.nameUUIDFromBytes("$from$to$summary".toByteArray()))
            out.println("DTSTART;VALUE=DATE:" + BASIC_ISO_DATE.format(from))
            out.println("DTEND;VALUE=DATE:" + BASIC_ISO_DATE.format(to))
            out.println("SUMMARY:" + summary.replace("\n", "\\n"))
//            out.println("DESCRIPTION:description_description")
            out.println("END:VEVENT")
        }
        out.println("END:VCALENDAR")
    }
}

private val ISO_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")