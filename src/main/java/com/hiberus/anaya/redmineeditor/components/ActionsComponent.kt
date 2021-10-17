package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.model.Model
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType

/**
 * A list of action buttons
 */
internal class ActionsComponent : BaseComponent() {

    override fun init() = Unit // nothing to initialize

    /* ------------------------- buttons ------------------------- */

    /**
     * press the refresh button to reload the data, asks if there are changes
     */
    @FXML
    private fun reload() = controller.runForeground { model: Model ->
        if (model.hasChanges()) {
            // if there are changes, ask first
            Alert(
                Alert.AlertType.WARNING,
                "There are unsaved changes, do you want to lose them and reload?",
                ButtonType.YES, ButtonType.CANCEL
            ).run {
                title = "Warning"
                headerText = "Unsaved changes"

                // display
                showAndWait()

                // stop if the user didn't accept
                if (result != ButtonType.YES) return@runForeground
            }
        }

        // either no changes, or the user did want to lose them
        forceReload()
    }

    /**
     * Uploads the data, then reloads
     */
    @FXML
    private fun upload() = controller.runBackground(
        { it.uploadAll() }, // let it upload
        { ok -> if (ok) forceReload() } // then reload if everything was ok
    )

    /* ------------------------- internal ------------------------- */

    /**
     * reloads the data (loses changes, if existing)
     */
    private fun forceReload() = controller.runBackground { model ->
        // clear data
        model.clearAll()

        // notify so that the ui is updated at this step
        controller.fireChanges()

        // load month
        model.loadMonth()
    }

}
