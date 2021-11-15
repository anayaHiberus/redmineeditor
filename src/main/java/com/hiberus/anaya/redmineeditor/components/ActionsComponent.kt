package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.settings.LoadSettings
import com.hiberus.anaya.redmineeditor.utils.confirmLoseChanges
import com.hiberus.anaya.redmineeditor.utils.hiberus.LoadSpecialDays
import com.hiberus.anaya.redmineeditor.utils.runInForeground
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.stage.Window
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

        // when app starts
        Platform.runLater {
            reload(reloadConfig = true, resetDay = true)
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
        if (!model.hasChanges || confirmLoseChanges("reload")) reload(reloadConfig = true)
    }

    /**
     * Uploads the data, then reloads
     */
    @FXML
    private fun upload() = AppController.runBackground(
        { it.uploadAll() }, // let it upload
        { reload() } // then reload (even if there were errors) TODO: on errors, try to keep them or something
    )

    /* ------------------------- internal ------------------------- */

    /**
     * reloads the data (loses changes, if existing)
     * Also reloads configuration if [reloadConfig] is set
     * Also resets the day if [resetDay] is set
     */
    private fun reload(reloadConfig: Boolean = false, resetDay: Boolean = false) {
        var settingsERROR = false
        var specialDaysERROR = false
        AppController.runBackground({ model ->

            // reload files
            if (reloadConfig) {
                settingsERROR = !LoadSettings()
                specialDaysERROR = !LoadSpecialDays()

                runInForeground {
                    // stylize displayed windows (should only be the main one)
                    Window.getWindows().map { it.scene }.distinct().forEach { it.stylize() }
                }
            }

            // set now
            if (resetDay) model.toNow()

            // reload data
            // TODO: don't reload when uploading, update internal state
            model.reloadRedmine(clearOnly = settingsERROR)

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
                    stylize()
                }.showAndWait()
            }
            if (specialDaysERROR) {
                // invalid special days, warning
                Alert(Alert.AlertType.WARNING).apply {
                    title = "Special days error"
                    contentText = "No valid special days data found"
                    stylize()
                }.showAndWait()
            }
        }
    }

    /**
     * On window closes, asks to lose changes if any
     */
    private fun closeWindowEvent(event: WindowEvent) {
        AppController.runForeground { model ->
            if (model.hasChanges && !confirmLoseChanges("exit")) event.consume()
        }
    }

}
