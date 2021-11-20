package com.hiberus.anaya.redmineeditor.summary

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import java.time.format.DateTimeFormatter
import java.util.*

fun DisplaySummary() {
    var text = "can't load"
    AppController.runBackground({ model ->
        text = buildString {
            appendLine("Mes: ${model.month.format(DateTimeFormatter.ofPattern("LLLL uuuu", Locale.forLanguageTag("es")))}")
            appendLine("Datos trabajador: " + model.user)
            appendLine()
            appendLine()
            model.monthEntries?.let { entries ->
                val projects = entries.map { it.issue }.distinct()
                    .groupBy { it.project }

                // TODO: allow selecting which projects to generate

                projects.onEachIndexed { i, (project, issues) ->
                    val index = i + 1
                    appendLine("Código proyecto $index: ${issues.first().getProjectOT()} ($project)")
                    appendLine("Descripción de la actividad $index:")
                    append("[reword] ")
                    appendLine(entries.asSequence().filter { it.issue.project == project }.map { it.comment }.distinct()
                        .filter { it.isNotBlank() }.map { it.trim().removeSuffix(".") + "." }
                        .joinToString(" ")
                    )
                    appendLine()
                    appendLine("1. Tarea realizada")
                    issues.forEach { issue ->
                        appendLine("#${issue.id} - ${issue.subject}")
                        appendLine(issue.url)
                    }
                    appendLine()
                    appendLine("2. Archivos de configuración asociados")
                    appendLine("[fill with pictures]")
                    appendLine()
                    appendLine()
                }

            }
        }
    }) {
        Alert(Alert.AlertType.INFORMATION).apply {
            headerText = "Summary"
            dialogPane.content = TextArea(text).apply { isWrapText = true }
            stylize()
        }.showAndWait()
    }
}