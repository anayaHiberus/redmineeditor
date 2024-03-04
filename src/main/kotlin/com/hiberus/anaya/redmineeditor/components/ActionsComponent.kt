package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.layout.HBox
import javafx.stage.WindowEvent

/**
 * A list of action buttons
 */
internal class ActionsComponent {

    @FXML
    lateinit var save: HBox

    @FXML
    lateinit var reload: Button

    /* ------------------------- init ------------------------- */

    @FXML
    fun initialize() {
        // register closing
        Platform.runLater {
            save.scene.window.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent) // attached to save because it's one valid element
        }

        // when data changes
        AppController.onChanges(
            setOf(
                ChangeEvent.EntryList, ChangeEvent.EntryContent, ChangeEvent.IssueContent
            )
        ) {
            save.enabled = it.hasChanges
            reload.text = if (it.hasChanges) "Discard and _Reload" else "_Reload"
        }
    }

    /* ------------------------- buttons ------------------------- */

    /**
     * press the refresh button to reload the data, asks if there are changes
     */
    @FXML
    private fun reload() = AppController.reload()

    /**
     * Uploads the data, then exits (if [exit]=true) or reloads otherwise
     */
    private fun upload(exit: Boolean) = AppController.runBackground(
        { it.uploadAll() }, // let it upload
        { correct ->
            if (correct) {
                // all done! exit or reload
                if (exit) Platform.exit()
                else AppController.reload(askIfChanges = false)
            } else {
                // an error occurred, ask what to do
                // TODO: on errors, try to keep them or something
                Alert(Alert.AlertType.ERROR).apply {
                    headerText = "Error"
                    contentText = "There was an error uploading changes. Unfortunately the app is not yet ready to recover in such cases.\nWhat do you want to do?"
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
        }
    )

    @FXML
    private fun uploadReload() = upload(false)

    @FXML
    private fun uploadExit() = upload(true)

    /* ------------------------- internal ------------------------- */

    /**
     * On window closes, asks to lose changes if any
     */
    private fun closeWindowEvent(event: WindowEvent) = AppController.runForeground { model ->
        if (model.hasChanges && !confirmLoseChanges("exit")) event.consume()
    }


}
