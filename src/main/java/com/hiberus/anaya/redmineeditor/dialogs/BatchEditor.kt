package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.components.describe
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.days
import com.hiberus.anaya.redmineeditor.utils.expectedHours
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
 * Show the batch editor dialog
 */
fun ShowBatchEditorDialog() {
    Stage().apply {
        title = "Batch editor"
        initModality(Modality.APPLICATION_MODAL)
        scene = Scene(FXMLLoader(ResourceLayout("batch_editor")).load())
        scene.stylize()
    }.showAndWait()
}

/**
 * The batch editor dialog controller
 */
class BatchEditorController {

    /* ------------------------- elements ------------------------- */

    @FXML
    lateinit var editor: TextArea

    @FXML
    lateinit var info: Label

    /* ------------------------- utils ------------------------- */

    /**
     * set the text info
     */
    private fun setInfo(text: String) = Platform.runLater { info.text = text }

    /* ------------------------- callbacks ------------------------- */

    @FXML
    // on init, get and show data
    fun initialize() = AppController.runBackground { editor.text = exportData(it) }

    @FXML
    fun test() = AppController.runBackground {
        // import in testing mode
        setInfo("Testing...")
        val errors = importData(editor.text, it, test = true)
        setInfo(errors.ifEmpty { "Valid content" })
    }

    @FXML
    fun doImport() = AppController.runBackground {
        // import
        setInfo("Importing...")
        val errors = importData(editor.text, it)
        setInfo(errors.ifEmpty { "Imported" })
    }

    @FXML
    fun close() = editor.scene.window.apply { fireEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST)) }

}

/* ------------------------- export/import ------------------------- */

val INSTRUCTIONS = """
    | Instructions:
    |  - Each non-empty line that doesn't start with '#' is considered an entry.
    |  - You can modify existing ones by keeping the entry id, or create new ones by using '+' as the entry id
""".trimMargin("|").split("\n").joinToString("\n") { "# $it" }

/**
 * Exports data from the app model
 */
private fun exportData(model: Model): String {
    // get data
    val entries = (model.monthEntries
        ?: return "Internal error: no data available").filter { it.id != null } // TODO: allow editing created entries
    val month = model.month

    // header
    return INSTRUCTIONS + LB +
            LB +
            "# " + listOf(
        "Entry id",
        "spent year",
        "spent month",
        "spent day",
        "spent hours",
        "issue id",
        "comment"
    ).joinToString(SEP) + LB +
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
                }.joinToString("") +
            LB +
            // append entries too
            "# Loaded entries: " + LB +
            entries.map { it.issue }.distinct().joinToString("") { it.toShortString() + LB }
}

/**
 * Imports entries [data] into a [model]. In [test] mode the model is not edited (but errors still can happen).
 * Returns the list of error/warnings. Empty if everything was ok.
 */
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
                    id = if (columns[0] == "+") null else (columns[0].toIntOrNull()
                        ?: throw IllegalArgumentException("entry id must be an int (for existing entries) or '+' (for new entries)")),
                    spent_on = LocalDate.of(
                        columns[1].toIntOrNull() ?: throw IllegalArgumentException("year must be an int"),
                        columns[2].toIntOrNull() ?: throw IllegalArgumentException("month must be an int"),
                        columns[3].toIntOrNull() ?: throw IllegalArgumentException("day must be an int")
                    ),
                    spent = columns[4].toDoubleOrNull()?.takeIf { it >= 0 }
                        ?: throw IllegalArgumentException("spent time must be a non-negative double"),
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
        .forEachIndexed { line, entryData ->
            runCatching {
                // common data
                val issue = model.loadedIssues?.firstOrNull { it.id == entryData.issueId }
                    ?: throw IllegalArgumentException("No issue with id ${entryData.issueId} found")
                val id = entryData.id

                if (id == null) {
                    // no id: create new entry

                    if (!test) { // skip while testing
                        model.createTimeEntry(
                            date = entryData.spent_on,
                            spent = entryData.spent,
                            issue = issue,
                            comment = entryData.comment,
                        ).let { if (it == null) throw IllegalArgumentException("Can't create entry with data $entryData") }
                    }

                } else {
                    // existing id: find and update existing entry
                    val matchedEntries = entries.filter { it.id == id }
                    if (matchedEntries.isEmpty()) {
                        // no entries to update
                        throw IllegalArgumentException("No entry with id $id found, nothing to modify. If you want to create one use '+' as id.")
                    } else if (matchedEntries.size > 1) {
                        // multiple???
                        throw IllegalArgumentException("Internal error (multiple entries for id $id)")
                    } else {
                        // found, update
                        if (!test) { // unless testing
                            matchedEntries[0].apply {
                                spent_on = entryData.spent_on
                                changeSpent(entryData.spent)
                                this.issue = issue
                                comment = entryData.comment
                            }
                        }
                    }
                }

            }.onFailure {
                // error while importing line
                appendLine("Error on line $line: ${it.message}")
            }
        }

    // reload everything
    if (!test) ChangeEvent.values().forEach { model.registerExternalChange(it) }
}

/**
 * Temporal dataClass for evaluated lines
 */
class ImportEntry(val id: Int?, val spent_on: LocalDate, val spent: Double, val issueId: Int, val comment: String)

/**
 * Custom CSV line parser. Don't do this, kids.
 */
private fun lineParser(line: String) = mutableListOf<String>().apply {
    var column = "" // current building column
    var spaces = 0 // spaces found
    var slash = false // slash mode
    var string = false // string mode

    // parse each char
    for (char in line) {
        if (slash) {
            // previous char was a slash, add without checks
            column += " ".repeat(spaces)
            spaces = 0
            column += if (char == 'n') '\n' else char
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