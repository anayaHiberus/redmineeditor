package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.utils.ensureSuffix
import com.hiberus.anaya.redmineeditor.utils.findFile
import com.hiberus.anaya.redmineeditor.utils.formatHours
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Calculates and displays the evidences
 */
fun ShowEvidencesDialog() {
    // init properties
    val strings = Properties().apply { findFile("conf/Evidences.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
    fun string(key: String, default: String? = null) = strings.getProperty(key, default ?: key)

    var text = string("error", "Can't load properties")
    AppController.runBackground({ model ->
        text = buildString {

            // common header
            appendLine("${string("header.month")}: " + model.month.format(
                DateTimeFormatter.ofPattern(string("header.date", "LLLL uuuu"), Locale.forLanguageTag(string("header.locale", "en")))
            ))
            appendLine("${string("header.worker")}: " + model.user)
            appendLine()
            appendLine()

            // from all entries
            (model.monthEntries ?: return@runBackground)
                // in which you spent something
                .filter { it.spent > 0 }
                // group by issues
                .groupBy { it.issue }.entries
                // then by projects
                .groupBy { it.key.project }
                // and keep as [project, issues, entries]
                .map { (project, data) -> Triple(project, data.map { it.key }, data.flatMap { it.value }) }
                // sort them by total spent hours
                .sortedByDescending { (_, _, entries) -> entries.sumOf { it.spent } }
                // now, for each...
                .onEachIndexed { i, (project, issues, entries) ->

                    // project header
                    val index = i + 1
                    appendLine("${string("project.code")} $index: ${issues.first().getProjectOT()} ($project)")
                    appendLine("${string("project.description")} $index:")
                    appendLine(string("project.edit") + " " +
                            // concatenate all comments
                            entries
                                .sortedBy { it.spent_on } // sort by date
                                .map { it.comment }
                                .reversed().distinct().reversed() // remove duplicates, but keep the last element
                                .filter { it.isNotBlank() }.map { it.trim().ensureSuffix(".") }
                                .joinToString(" ")
                    )
                    appendLine()

                    // issues
                    appendLine(string("tasks"))
                    issues
                        .sortedByDescending { issue -> entries.filter { it.issue == issue }.sumOf { it.spent } }
                        .forEach {
                            appendLine("#${it.id} - ${it.subject}")
                            appendLine(it.url)
                        }
                    appendLine()

                    // hours
                    appendLine(string("hours"))
                    appendLine(
                        // group entries by date
                        entries.groupBy { it.spent_on }.toSortedMap()
                            // calculate total hours for each date
                            .mapValues { (_, dateEntries) -> dateEntries.sumOf { it.spent } }
                            // append each
                            .map { (date, hours) ->
                                " - $date: $hours (${hours.formatHours()})"
                            }.joinToString("\n")
                    )
                    appendLine(string("total") + " " + entries.sumOf { it.spent }.formatHours())
                    appendLine()

                    // pictures (placeholder)
                    appendLine(string("files"))
                    appendLine(string("files.edit"))
                    appendLine()
                    appendLine()

                }
        }
    }) {
        Alert(Alert.AlertType.INFORMATION).apply {
            headerText = "Evidences [beta]"
            dialogPane.content = TextArea(text).apply { isWrapText = true }
            stylize()
        }.showAndWait()
    }
}