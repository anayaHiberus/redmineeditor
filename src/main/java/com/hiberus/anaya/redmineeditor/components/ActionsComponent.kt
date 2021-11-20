package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.confirmLoseChanges
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.stage.WindowEvent

/**
 * A list of action buttons
 */
internal class ActionsComponent {

    @FXML
    lateinit var save: Button

    /* ------------------------- init ------------------------- */

    @FXML
    fun initialize() {
        // register closing
        Platform.runLater {
            save.scene.window.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent) // attached to save because it's one valid element
        }

        // when data changes
        AppController.onChanges(setOf(
            ChangeEvents.EntryList, ChangeEvents.EntryContent, ChangeEvents.IssueContent
        )) {
            save.isDisable = !it.hasChanges
        }
    }

    /* ------------------------- buttons ------------------------- */

    /**
     * press the refresh button to reload the data, asks if there are changes
     */
    @FXML
    private fun askReload() = AppController.runForeground { model: Model ->
        if (!model.hasChanges || confirmLoseChanges("reload")) AppController.reload()
    }

    /**
     * Uploads the data, then reloads
     */
    @FXML
    private fun upload() = AppController.runBackground(
        { it.uploadAll() }, // let it upload
        { AppController.reload() } // then reload (even if there were errors) TODO: on errors, try to keep them or something
    )

    /* ------------------------- internal ------------------------- */

    /**
     * On window closes, asks to lose changes if any
     */
    private fun closeWindowEvent(event: WindowEvent) {
        AppController.runForeground { model ->
            if (model.hasChanges && !confirmLoseChanges("exit")) event.consume()
        }
    }

}
