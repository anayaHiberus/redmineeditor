package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.controller.settingsLoaded
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.utils.getModuleResource
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import java.time.LocalDate
import java.time.YearMonth

/**
 * Component for the parent app. Manages the loading indicator
 */
internal class ParentComponent {

    /* ------------------------- elements ------------------------- */

    @FXML
    lateinit var progress: ProgressIndicator  // that circular thingy

    @FXML
    lateinit var parent: VBox // the main app, disabled while loading

    /* ------------------------- init ------------------------- */

    @FXML
    fun initialize() {
        // When loading, the indicator is shown and the whole app is disabled.
        AppController.onChanges(setOf(ChangeEvents.Loading)) {
            progress.isVisible = it.isLoading
            parent.isDisable = it.isLoading
        }


        // start the app
        AppController.runBackground({
            // load current day and month
            it.month = YearMonth.now()
            it.day = LocalDate.now().dayOfMonth
            AppController.fireChanges() // notify now to display month while loading
            it.loadMonth()
        }, {
            if (!settingsLoaded) {
                // invalid configuration, error
                Alert(Alert.AlertType.ERROR).apply {
                    contentText = "No valid configuration found"
                }.showAndWait()
            }
        })
    }

    /* ------------------------- readme ------------------------- */

    @FXML
    fun showReadme() = Alert(Alert.AlertType.INFORMATION).apply {
        headerText = "Readme"
        dialogPane.content = ScrollPane(Label(this@ParentComponent.javaClass.getModuleResource("Readme.txt").readText()))
    }.showAndWait()

}