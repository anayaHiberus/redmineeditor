package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.dialogs.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
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
            ChangeEvent.EntryList, ChangeEvent.EntryContent, ChangeEvent.IssueContent
        )) {
            save.enabled = it.hasChanges
        }
    }

    /* ------------------------- buttons ------------------------- */

    /**
     * press the refresh button to reload the data, asks if there are changes
     */
    @FXML
    private fun reload() = AppController.reload()

    /**
     * Uploads the data, then reloads
     */
    @FXML
    private fun upload() = AppController.runBackground(
        { it.uploadAll() }, // let it upload
        {
            // then ask to reload/exit (even if there were errors)
            // TODO: on errors, try to keep them or something
            // TODO: add setting to automatically do one without asking (configurable as 'ask, exit, reload' and this dialog should have a 'always do this without asking' checkbox that should show 'you can change this from settings' and then changes the setting)
            Alert(if (it) Alert.AlertType.CONFIRMATION else Alert.AlertType.ERROR).apply {
                headerText = if (it) "Uploaded" else "Error"
                contentText = (if (it) "The changes where uploaded correctly." else "There was an error uploading changes. Unfortunately the app is not yet ready to recover in such cases.") + "\nWhat do you want to do now?"
                stylize()
                clearButtons()
                addButton(ButtonType("Reload")) {
                    AppController.reload(askIfChanges = false)
                }
                addButton(ButtonType("Exit")) {
                    Platform.exit()
                }
            }.showAndWait()
        }
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
