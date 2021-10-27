package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.controller.MyException
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.event.Event
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import java.io.IOException
import java.net.URI
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
            edTxt_comment.text = comment

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
        // no need to notify
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
            AppController.fireChanges(setOf(ChangeEvents.Hours))
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
            AppController.fireChanges(setOf(ChangeEvents.Hours))
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
            AppController.fireChanges(setOf(ChangeEvents.Hours))
        }

    /**
     * load spent hours
     */
    @FXML
    private fun loadTotal() = AppController.runBackground { model: Model.Editor ->
        item?.issue?.run {
            try {
                downloadSpent()
                model.registerExternalChange(ChangeEvents.Hours)
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

            // button
            buttonTypes += OPEN_BUTTON
            buttonTypes += ButtonType.CLOSE // to allow closing by pressing the 'x' button

        }.showAndWait() // display

        if (result.resultButton == OPEN_BUTTON) {
            // if open pressed, open in desktop
            thread(isDaemon = true) {
                URI(issue.url).openInBrowser().ifNotOK {
                    // on error, display alert
                    Platform.runLater {
                        Alert(Alert.AlertType.ERROR).apply {
                            contentText = "Couldn't open the browser"
                        }.showAndWait()
                    }
                }
            }
        }
    }

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

