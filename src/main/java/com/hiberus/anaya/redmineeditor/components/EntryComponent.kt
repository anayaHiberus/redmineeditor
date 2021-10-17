package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineapi.dNONE
import com.hiberus.anaya.redmineapi.dUNINITIALIZED
import com.hiberus.anaya.redmineeditor.controller.Controller
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
import javafx.scene.web.WebView
import java.io.IOException
import java.net.URI
import kotlin.concurrent.thread

/**
 * One of the entries in the entries list
 */
class EntryComponent : SimpleListCell<TimeEntry> {

    /* ------------------------- views ------------------------- */

    @FXML
    lateinit var spent_sub: HBox

    @FXML
    lateinit var issue: Label

    @FXML
    lateinit var comment: TextField

    @FXML
    lateinit var spent: Label

    @FXML
    lateinit var get_total: Button

    @FXML
    lateinit var sync_realization: Button

    @FXML
    lateinit var total: Label

    @FXML
    lateinit var estimated: Label

    @FXML
    lateinit var estimated_sub: HBox

    @FXML
    lateinit var realization: Label

    @FXML
    lateinit var realization_sub: HBox

    @FXML
    lateinit var realization_add: HBox

    /* ------------------------- init ------------------------- */

    private val controller: Controller

    private constructor() : this(Controller()) // to avoid error on fxml file

    /**
     * creates new cell
     */
    constructor(controller: Controller) : super("entry_cell.fxml") {
        this.controller = controller

        // remove when invisible
        get_total.syncInvisible()
        total.syncInvisible()
        sync_realization.syncInvisible()
    }

    public override fun update() {
        // sets the cell data
        val entry = item ?: return
        val issue = entry.issue

        // --- issue ---
        val issue_spent = issue.spent
        val issue_estimated = issue.estimated

        // text
        this.issue.text = issue.toString()

        // estimated
        estimated.text = when (issue_estimated) {
            dUNINITIALIZED -> "?"
            dNONE -> "none"
            else -> issue_estimated.formatHours()
        }
        estimated_sub.isDisable = issue_estimated < 0

        // spent
        get_total.isVisible = issue_spent == dUNINITIALIZED
        total.isVisible = issue_spent != dUNINITIALIZED
        total.text = if (issue_spent < 0) "none" else issue_spent.formatHours()

        // sync spent-realization
        if (issue_spent >= 0 && issue_estimated > 0) {
            // show and allow sync
            total.text = "${total.text} | ${(issue_spent / issue_estimated * 100).toInt()}%"
            sync_realization.isVisible = true
        } else {
            // disable sync
            sync_realization.isVisible = false
        }

        // realization
        val realization = issue.realization
        this.realization.text = "$realization%"
        realization_add.isDisable = realization >= 100
        realization_sub.isDisable = realization <= 0

        // --- entry ---
        val spent = entry.spent

        // spent
        this.spent.text = spent.formatHours()
        spent_sub.isDisable = spent <= 0

        // comment
        comment.text = entry.comment

        // general
        this.opacity = if (spent <= 0) 0.5 else 1.0 // TODO: change different opacity of modified issues
    }

    /* ------------------------- actions ------------------------- */

    @FXML
    private fun changedComment() {
        // update comment
        item?.comment = comment.text
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
            controller.fireChanges(setOf(ChangeEvents.Hours))
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
            controller.fireChanges(setOf(ChangeEvents.Hours))
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
            controller.fireChanges(setOf(ChangeEvents.Hours))
        }

    /**
     * load spent hours
     */
    @FXML
    private fun getTotal() = controller.runBackground { model: Model.Editor ->
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
            buttonTypes += ButtonType(OPEN_BUTTON)

        }.showAndWait() // display

        if (result?.takeIf { it.isPresent }?.get()?.text == OPEN_BUTTON) {
            // if open pressed, open in desktop
            thread {
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
 * Button string to open in redmine
 */
const val OPEN_BUTTON = "Open in Redmine"

/**
 * Get the userdata of the target node
 */
private val Event.targetData: String
    get() = (target as Node).userData.toString()

