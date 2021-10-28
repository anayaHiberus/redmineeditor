package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.controller.LoadSettings
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.hiberus.LoadSpecialDays
import com.hiberus.anaya.redmineeditor.utils.resultButton
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import java.time.LocalDate
import java.time.YearMonth

/**
 * A list of action buttons
 */
internal class ActionsComponent {

    /* ------------------------- init ------------------------- */

    @FXML
    fun initialize() {
        AppController.onChanges(setOf(ChangeEvents.Start)) { model ->
            // init data
            model.day = LocalDate.now().dayOfMonth
            model.month = YearMonth.now()
            // start the app
            forceReload(true)
        }
    }

    /* ------------------------- buttons ------------------------- */

    /**
     * press the refresh button to reload the data, asks if there are changes
     */
    @FXML
    private fun reload() = AppController.runForeground { model: Model ->
        if (model.hasChanges == true) {
            // if there are changes, ask first
            val result = Alert(
                Alert.AlertType.WARNING,
                "There are unsaved changes, do you want to lose them and reload?",
                ButtonType.YES, ButtonType.CANCEL
            ).apply {
                title = "Warning"
                headerText = "Unsaved changes"

            }.showAndWait() // display

            // stop if the user didn't accept
            if (result.resultButton != ButtonType.YES) return@runForeground
        }

        // either no changes, or the user did want to lose them
        forceReload(true)
    }

    /**
     * Uploads the data, then reloads
     */
    @FXML
    private fun upload() = AppController.runBackground(
        { it.uploadAll() }, // let it upload
        { ok -> if (ok) forceReload() } // then reload if everything was ok
    )

    /* ------------------------- internal ------------------------- */

    /**
     * reloads the data (loses changes, if existing)
     * Also reloads configuration if [reloadConfig] is set
     */
    private fun forceReload(reloadConfig: Boolean = false) {
        var settingsERROR = false
        var specialDaysERROR = false
        AppController.runBackground({ model ->

            if (reloadConfig) {
                // reload files
                settingsERROR = !LoadSettings()
                specialDaysERROR = !LoadSpecialDays()

                // reload data
                model.reloadRedmine(clearOnly = settingsERROR)
            }

            // notify so that the ui is updated at this step and everything is updated
            AppController.fireChanges()

            // load month
            model.loadDate()

        }) {
            // after loading
            if (settingsERROR) {
                // invalid configuration, error
                Alert(Alert.AlertType.ERROR).apply {
                    title = "Configuration error"
                    contentText = "No valid configuration found"
                }.showAndWait()
            }
            if (specialDaysERROR) {
                // invalid special days, warning
                Alert(Alert.AlertType.WARNING).apply {
                    title = "Special days error"
                    contentText = "No valid special days data found"
                }.showAndWait()
            }
        }
    }

}
