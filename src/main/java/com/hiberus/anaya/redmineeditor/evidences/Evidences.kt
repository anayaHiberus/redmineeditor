package com.hiberus.anaya.redmineeditor.evidences

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.utils.ensureSuffix
import com.hiberus.anaya.redmineeditor.utils.formatHours
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Calculates and displays the evidences
 * TODO: extract all strings to file
 */
fun DisplayEvidences() {
    var text = "Can't load"
    AppController.runBackground({ model ->
        text = buildString {

            // common header
            appendLine("Mes: ${model.month.format(DateTimeFormatter.ofPattern("LLLL uuuu", Locale.forLanguageTag("es")))}")
            appendLine("Datos trabajador: " + model.user)
            appendLine()
            appendLine()

            // from all entries
            (model.monthEntries ?: return@runBackground)
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
                    appendLine("Código proyecto $index: ${issues.first().getProjectOT()} ($project)")
                    appendLine("Descripción de la actividad $index:")
                    append("[reword] ")
                    // concatenate all comments
                    appendLine(entries.asSequence().map { it.comment }.distinct()
                        .filter { it.isNotBlank() }.map { it.trim().ensureSuffix(".") }
                        .joinToString(" ")
                    )
                    appendLine()

                    // issues
                    appendLine("1. Tarea realizada")
                    issues.sortedBy { it.id }.forEach {
                        appendLine("#${it.id} - ${it.subject}")
                        appendLine(it.url)
                    }
                    appendLine()

                    // hours
                    appendLine("Resumen de horas imputadas:")
                    appendLine(
                        // group entries by date
                        entries.groupBy { it.spent_on }.toSortedMap()
                            // calculate total hours for each date
                            .mapValues { (_, dateEntries) -> dateEntries.sumOf { it.spent } }
                            // skip days with zero hours
                            .filterValues { it > 0 }
                            // append each
                            .map { (date, hours) ->
                                " - $date: $hours (${hours.formatHours()})"
                            }.joinToString("\n")
                    )
                    appendLine()

                    // pictures (placeholder)
                    appendLine("2. Archivos de configuración asociados")
                    appendLine("[fill with pictures]")
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