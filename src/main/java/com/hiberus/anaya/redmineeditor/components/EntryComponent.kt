package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.controller.MyException
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.event.Event
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import java.io.IOException
import java.net.URI
import java.util.function.Consumer
import kotlin.concurrent.thread


/**
 * One of the entries in the entries list
 */
@Suppress("unused")
class EntryComponent : SimpleListCell<TimeEntry>("entry_cell.fxml") {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var box_issue: HBox

    @FXML
    lateinit var txt_details: Label

    @FXML
    lateinit var txt_estimated: Label

    @FXML
    lateinit var sub_estimated: HBox

    @FXML
    lateinit var btn_total: Button

    @FXML
    lateinit var txt_total: Label

    @FXML
    lateinit var btn_sync: Button

    @FXML
    lateinit var txt_realization: Label

    @FXML
    lateinit var add_realization: HBox

    @FXML
    lateinit var sub_realization: HBox

    @FXML
    lateinit var box_entry: HBox

    @FXML
    lateinit var txt_spent: Label

    @FXML
    lateinit var sub_spent: HBox

    @FXML
    lateinit var edTxt_comment: TextField

    /* ------------------------- init ------------------------- */

    init {
        // remove when invisible
        btn_total.syncInvisible()
        txt_total.syncInvisible()
        btn_sync.syncInvisible()
    }

    public override fun update() {
        // sets the cell data
        // TODO: mark each individual modified setting, or show a dialog when pressing save with option to revert (maybe even checkboxes)

        // TODO: separate updateIssue and updateEntry

        // --- issue ---
        item?.issue?.apply {

            // text
            txt_details.text = toString()

            // estimated
            txt_estimated.text = estimated?.formatHours() ?: "none"
            sub_estimated.isDisable = estimated == null

            // spent
            btn_total.isVisible = spent == null
            txt_total.isVisible = spent != null
            txt_total.text = spent?.formatHours() ?: "none"

            // sync spent-realization
            spent_realization?.let {
                // show and allow sync
                txt_total.text += " | $it%"
                txt_total.backgroundColor = if (it > 100) Color.INDIANRED else null
                btn_sync.isVisible = true
                btn_sync.isDisable = it == realization
            } ?: run {
                // disable sync
                txt_total.backgroundColor = null
                btn_sync.isVisible = false
            }

            // realization
            txt_realization.text = "$realization%"
            txt_realization.backgroundColor = spent_realization?.let { if (it > realization) Color.ORANGE else null }
            add_realization.isDisable = realization >= 100
            sub_realization.isDisable = realization <= 0

            // container
            box_issue.opacity = when {
                requiresUpload -> 1.0 // need to upload
                item?.run { spent > 0 } == true -> 0.75 // entry used today
                else -> 0.5 // other
            }

        }

        // --- entry ---
        item?.apply {

            // spent
            txt_spent.text = spent.formatHours()
            sub_spent.isDisable = spent <= 0

            // comment
            comment.takeIf { it != edTxt_comment.text }?.let { edTxt_comment.text = it } // don't update if the same, to avoid moving the caret

            // container
            box_entry.opacity = when {
                requiresUpload -> 1.0 // need to upload
                spent > 0 -> 1.0 // used today
                else -> 0.5 // other
            }

        }

    }

    /* ------------------------- actions ------------------------- */

    @FXML
    private fun changedComment() {
        // update comment
        item?.comment = edTxt_comment.text
        // and notify
        AppController.fireChanges(setOf(ChangeEvents.EntryContent))
    }

    /**
     * increase or decrease this entry hours
     */
    @FXML
    private fun changeSpent(node: Event) =
        item?.run {
            // update entry
            addSpent(node.targetData.toDouble()) // the button data is the amount
            // and notify
            AppController.fireChanges(setOf(ChangeEvents.EntryContent, ChangeEvents.DayHours))
        }

    /**
     * edits the entry hours with an editor
     */
    @FXML
    private fun editSpent() =
        item?.run {
            // editor
            showHoursEditor("Spent", "0h", spent.toString()) {
                // update entry
                runCatching {
                    changeSpent(it.takeIf { it.isNotBlank() }?.toDouble() ?: 0.0)
                }
            }
            // and notify
            AppController.fireChanges(setOf(ChangeEvents.EntryContent, ChangeEvents.DayHours))
        }

    /**
     * increase or decrease the issue estimated hours
     */
    @FXML
    private fun changeEstimated(node: Event) =
        item?.issue?.run {
            // update issue entry
            addEstimated(node.targetData.toDouble()) // the button data is the amount
            // and notify
            AppController.fireChanges(setOf(ChangeEvents.IssueContent))
        }

    /**
     * edits the estimated hours with an editor
     */
    @FXML
    private fun editEstimated() =
        item?.issue?.run {
            // editor
            showHoursEditor("Estimated", "none", estimated?.toString() ?: "") {
                // update entry
                runCatching {
                    changeEstimated(it.takeIf { it.isNotBlank() }?.toDouble())
                }
            }
            // and notify
            AppController.fireChanges(setOf(ChangeEvents.IssueContent))
        }

    /**
     * increase, decrease or sync the issue realization percentage
     */
    @FXML
    private fun changeRealization(node: Event) =
        item?.issue?.run {
            when (val data = node.targetData) {
                // sync button
                ">" -> syncRealization()
                // offset button
                else -> addRealization(data.toInt()) // the button data is the amount
            }
            // and notify
            AppController.fireChanges(setOf(ChangeEvents.IssueContent))
        }

    /**
     * load spent hours
     */
    @FXML
    private fun loadTotal() = AppController.runBackground { model: Model.Editor ->
        item?.issue?.run {
            try {
                if (downloadSpent())
                    model.registerExternalChange(ChangeEvents.IssueContent)
            } catch (e: IOException) {
                throw MyException("Network error", "Unable to fetch the issue details", e)
            }
        }
    }

    @FXML
    private fun showDetails() {
        // TODO: improve
        val issue = item?.issue ?: return


        // build alert
        val result = Alert(Alert.AlertType.INFORMATION).apply {
            title = issue.toShortString()
            headerText = issue.toString()
            if (issue.description.isNotEmpty()) {
                // html description
                dialogPane.content = WebView().apply { engine.loadContent(issue.description) }
            } else {
                // no description
                contentText = "no description"
            }

            // buttons
            buttonTypes.setAll(OPEN_BUTTON, ButtonType.CLOSE)

            stylize()
        }.showAndWait() // display

        if (result.resultButton == OPEN_BUTTON) {
            // if open pressed, open in desktop
            thread(isDaemon = true) {
                URI(issue.url).openInBrowser().ifNotOK {
                    // on error, display alert
                    Platform.runLater {
                        Alert(Alert.AlertType.ERROR).apply {
                            contentText = "Couldn't open the browser"
                            stylize()
                        }.showAndWait()
                    }
                }
            }
        }
    }

    @FXML
    fun copyToToday() = AppController.runBackground { model ->
        item?.let {
            // set now and copy entry
            model.toNow()
            model.copyTimeEntry(it)
        }
    }

    // TODO: add a 'delete' entry, think about what to do if the entry exists

}

/* ------------------------- utils ------------------------- */

/**
 * Button to open in redmine
 */
val OPEN_BUTTON = ButtonType("Open in Redmine")

/**
 * Get the userdata of the target node
 */
private val Event.targetData: String
    get() = (target as Node).userData.toString()

/**
 * Displays an editor to change an hours entry
 */
private fun showHoursEditor(label: String, ifEmpty: String, initialValue: String, onResult: Consumer<in String>) {
    TextInputDialog(initialValue).apply {
        title = "Hours raw editor"
        contentText = "$label:"
        editor.onKeyTyped = EventHandler<KeyEvent> {
            headerText = editor.text.takeIf { it.isNotBlank() }
                ?.runCatching { "$label: ${toDouble().formatHours()}" }?.getOrElse { "Invalid input" }
                ?: "$label: $ifEmpty"
        }.also { it.handle(null) } // also, run it now
        stylize()
    }.showAndWait().ifPresent(onResult)
}