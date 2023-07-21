package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.dialogs.FixMonthTool
import com.hiberus.anaya.redmineeditor.dialogs.MyException
import com.hiberus.anaya.redmineeditor.dialogs.showDetails
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.event.Event
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.TextInputDialog
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import java.io.IOException
import java.util.function.Consumer


/**
 * One of the entries in the entries list
 */
class EntryComponent : SimpleListCell<TimeEntry>(ResourceLayout("entry_cell")) {

    /* ------------------------- views ------------------------- */

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
    lateinit var txt_spent: Label

    @FXML
    lateinit var max_spent: Button

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

    override fun update() = partialUpdate(updateIssue = true, updateEntry = true)

    /**
     * Updates the content of issue or entry
     */
    fun partialUpdate(updateIssue: Boolean = false, updateEntry: Boolean = false) {

        // sets the cell data
        // TODO: mark each individual modified setting, or show a dialog when pressing save with option to revert (maybe even checkboxes)

        // --- issue ---
        item?.takeIf { updateIssue }?.issue?.apply {

            // text
            txt_details.text = toString()
            txt_details.backgroundColor = color?.withOpacity(0.5)

            // estimated
            txt_estimated.text = estimated?.formatHours() ?: "none"
            sub_estimated.enabled = estimated != null
            txt_estimated.bold = changedEstimation

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
                btn_sync.enabled = it != realization
            } ?: run {
                // disable sync
                txt_total.backgroundColor = null
                btn_sync.isVisible = false
            }

            // realization
            txt_realization.text = "$realization%"
            txt_realization.backgroundColor = spent_realization?.let { if (it > realization) Color.ORANGE else null }
            add_realization.enabled = realization < 100
            sub_realization.enabled = realization > 0
            txt_realization.bold = changedRealization

        }

        // --- entry ---
        item?.takeIf { updateEntry }?.apply {

            // spent
            txt_spent.text = spent.formatHours()
            max_spent.enabled = AppController.runForeground { model -> model.getPending()?.let { it > 0 } ?: false }
            sub_spent.enabled = spent > 0
            txt_spent.bold = changedSpent

            // comment
            comment.takeIf { it != edTxt_comment.text }?.let { edTxt_comment.text = it } // don't update if the same, to avoid moving the caret
            edTxt_comment.bold = changedComment

            // mark entries if they have spent time
            this@EntryComponent.style = if (spent > 0) "-fx-control-inner-background: #A0A0A0;" else null

    }

    }

    /* ------------------------- actions ------------------------- */

    @FXML
    private fun changedComment() {
        // update comment
        item?.comment = edTxt_comment.text
        // and notify
        AppController.fireChanges(setOf(ChangeEvent.EntryContent))
    }

    /**
     * increase or decrease this entry hours
     */
    @FXML
    private fun changeSpent(node: Event) =
        item?.run {
            when (val data = node.targetData) {
                // add all button
                "max" -> AppController.runForeground { model -> model.getPending()?.let { addSpent(it) } }
                // substract all button
                "min" -> AppController.runForeground { model -> model.getPending()?.let { if (it < 0) addSpent(it) else changeSpent(0.0) } }
                // offset button
                else -> addSpent(data.toDouble()) // the button data is the amount
            }

            // and notify
            AppController.fireChanges(setOf(ChangeEvent.EntryContent, ChangeEvent.DayHours))
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
            AppController.fireChanges(setOf(ChangeEvent.EntryContent, ChangeEvent.DayHours))
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
            AppController.fireChanges(setOf(ChangeEvent.IssueContent))
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
                    estimated = it.takeIf { it.isNotBlank() }?.toDouble()
                }
            }
            // and notify
            AppController.fireChanges(setOf(ChangeEvent.IssueContent))
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
            AppController.fireChanges(setOf(ChangeEvent.IssueContent))
        }

    @FXML
    private fun loadTotal() = loadExtra { }

    /**
     * load spent hours and journal
     */
    private fun loadExtra(later: (Boolean) -> Unit) = AppController.runBackground({ model: Model.Editor ->
        item?.issue?.run {
            try {
                if (downloadExtra())
                    model.registerExternalChange(ChangeEvent.IssueContent)
            } catch (e: IOException) {
                throw MyException("Network error", "Unable to fetch the issue details", e)
            }
        }
    }, later)

    @FXML
    // load and show issue details
    private fun showDetails() = item?.issue?.apply { loadExtra { showDetails() } }

    @FXML
    fun copyToToday() = AppController.runBackground { model ->
        item?.let {
            // set now and copy entry
            model.toNow()
            model.copyTimeEntry(it)
        }
    }

    @FXML
    fun fixMonth() = AppController.runBackground {
        FixMonthTool(it, listOf(item.issue to item.comment), selectedWeek = false, futureDays = true)
    }

    @FXML
    fun clear() {
        // clear hours and comment
        item.changeSpent(0.0)
        edTxt_comment.text = ""
        item.comment = ""

        // and notify
        AppController.fireChanges(setOf(ChangeEvent.EntryContent, ChangeEvent.DayHours))
    }

}

/* ------------------------- utils ------------------------- */

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
