package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.components.describe
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.days
import com.hiberus.anaya.redmineeditor.utils.expectedHours
import com.hiberus.anaya.redmineeditor.utils.ifNotOK
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import java.time.LocalDate

private const val SEP = ", "
private const val LB = "\n"

/**
 * Calculates and displays the evidences
 */
fun BatchEditor() {
    // get data
    var text = "Error exporting data"
    AppController.runBackground({ model ->
        text = exportData(model)
    }) {

        Stage().apply {
            title = "Batch editor"
            initModality(Modality.APPLICATION_MODAL)

            // initialize scene
            val loader = FXMLLoader(Resources.getLayout("batch_editor"))
            scene = Scene(loader.load()).apply { stylize() }
        }.showAndWait()

    }
}

class BatchEditorController {

    @FXML
    lateinit var editor: TextArea

    @FXML
    lateinit var info: Label

    fun setInfo(text: String) = Platform.runLater { info.text = text }

    @FXML
    fun initialize() = AppController.runBackground { editor.text = exportData(it) }

    @FXML
    fun test() = AppController.runBackground {
        setInfo("Testing...")
        val errors = importData(editor.text, it, test = true)
        setInfo(errors.ifEmpty { "Valid content" })
    }

    @FXML
    fun doImport() = AppController.runBackground {
        setInfo("Importing...")
        val errors = importData(editor.text, it)
        setInfo(errors.ifEmpty { "Imported" })
    }

    @FXML
    fun close() = editor.scene.window.apply { fireEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)) }

}

/**
 * Exports data from the app model
 */
private fun exportData(model: Model): String {
    // get data
    val entries = (model.monthEntries ?: return "Can't export data").filter { it.id != null } // TODO: allow editing created entries
    val month = model.month

    // header
    return "# " + listOf("Entry id", "spent year", "spent month", "spent day", "spent hours", "issue id", "comment").joinToString(SEP) + LB +
            LB +
            // for each day
            month.days()
                // except holidays
                .filter { it.expectedHours > 0 }
                // associate with the entries of that day
                .associateWith { day -> entries.filter { it.spent_on == day } }
                // and convert to text
                .map { (day, entries) ->
                    // day header
                    "# " + describe(day, entries.sumOf { it.spent }, day.expectedHours) + LB +
                            // and entries
                            entries
                                // sorted by spent amount
                                .sortedBy { it.spent }
                                // converted as
                                .joinToString("") {
                                    listOf(
                                        it.id,
                                        it.spent_on.year,
                                        it.spent_on.monthValue,
                                        it.spent_on.dayOfMonth,
                                        it.spent,
                                        it.issue.id,
                                        '"' + it.comment.replace("\n", "\\n").replace("\"", "\\\"") + '"'
                                    ).joinToString(SEP) + LB
                                } + LB
                }.joinToString("")
}

private fun importData(data: String, model: Model.Editor, test: Boolean = false) = buildString {

    // get entries
    val entries = model.monthEntries ?: run {
        appendLine("Internal error (no entries available)")
        return@buildString
    }


    // parse data
    data.lines()
        // parse CSV line
        .map { lineParser(it) }
        // check columns
        .mapIndexedNotNull { index, columns ->
            val COL = 7
            when {
                // empty line
                columns.isEmpty() -> null
                // valid line
                columns.size == COL -> index + 1 to columns
                // invalid line
                else -> null.also { appendLine("Invalid number of columns in line $index. Required $COL, found ${columns.size}") }
            }
        }
        // parse columns
        .mapNotNull { (index, columns) ->
            runCatching {
                ImportEntry(
                    id = if (columns[0] == "+") null else (columns[0].toIntOrNull() ?: throw IllegalArgumentException("entry id must be an int (for existing entries) or '+' (for new entries)")),
                    spent_on = LocalDate.of(
                        columns[1].toIntOrNull() ?: throw IllegalArgumentException("year must be an int"),
                        columns[2].toIntOrNull() ?: throw IllegalArgumentException("month must be an int"),
                        columns[3].toIntOrNull() ?: throw IllegalArgumentException("day must be an int")
                    ),
                    spent = columns[4].toDoubleOrNull()?.takeIf { it >= 0 } ?: throw IllegalArgumentException("spent time must be a non-negative double"),
                    issueId = columns[5].toIntOrNull() ?: throw IllegalArgumentException("issue_id must be an int"),
                    comment = columns[6],
                )
            }.getOrElse {
                // exception
                appendLine("Invalid data on line $index: ${it.message}")
                null // invalid
            }
        }
        // load issues
        .also { data ->
            model.loadIssues(data.map { it.issueId })
        }
        // import
        .forEachIndexed importLine@{ line, entryData ->
            runCatching {
                // common data
                val issue = model.loadedIssues?.firstOrNull { it.id == entryData.issueId } ?: throw IllegalArgumentException("No issue with id ${entryData.issueId} found")
                val id = entryData.id

                if (id == null) {
                    if (test) return@importLine

                    // create new entry
                    model.createTimeEntry(
                        date = entryData.spent_on,
                        spent = entryData.spent,
                        issue = issue,
                        comment = entryData.comment,
                    ).ifNotOK { throw IllegalArgumentException("Can't create entry with data $entryData") }

                } else {

                    // update existing entry
                    val matchedEntries = entries.filter { it.id == id }
                    if (matchedEntries.isEmpty()) {
                        // no entries to update
                        throw IllegalArgumentException("No entry with id $id found, nothing to modify. If you want to create one use '+' as id.")
                    } else if (matchedEntries.size > 1) {
                        // multiple???
                        throw IllegalArgumentException("Internal error (multiple entries for id $id)")
                    } else {
                        if (test) return@importLine

                        // update the entry
                        val entry = matchedEntries[0]

                        entry.spent_on = entryData.spent_on
                        entry.changeSpent(entryData.spent)
                        entry.issue = issue
                        entry.comment = entryData.comment
                    }

                }

            }.onFailure {
                appendLine("Error on line $line: ${it.message}")
            }
        }

    // reload everything
    if (!test) ChangeEvent.values().forEach { model.registerExternalChange(it) }
}

class ImportEntry(val id: Int?, val spent_on: LocalDate, val spent: Double, val issueId: Int, val comment: String)

private fun lineParser(line: String) = mutableListOf<String>().apply {
    var column = "" // current building column
    var spaces = 0 // spaces found
    var slash = false // slash mode
    var string = false // string mode

    // parse each char
    for (char in line) {
        if (slash) {
            // previous char was a slash, add without checks
            column += char
            slash = false
        } else if (string && char != '"') {
            // in string mode, add without check
            column += char
        } else when (char) {
            // space
            ' ' -> {
                if (column.isNotEmpty()) spaces++
            }
            // new column
            ',' -> {
                add(column)
                column = ""
                spaces = 0
            }
            // toggle string mode
            '"' -> {
                column += " ".repeat(spaces)
                spaces = 0
                string = !string
            }
            // set slash mode
            '\\' -> {
                column += " ".repeat(spaces)
                spaces = 0
                slash = true
            }
            // comment, ignore rest
            '#' -> break
            // other, add
            else -> {
                column += " ".repeat(spaces)
                spaces = 0
                column += char
            }
        }
    }

    // add last column
    if (column.isNotBlank()) add(column)
}