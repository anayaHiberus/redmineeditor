package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import com.hiberus.anaya.redmineeditor.utils.getModuleResource
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox


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

    }

    /* ------------------------- readme ------------------------- */

    @FXML
    fun showReadme() = Alert(Alert.AlertType.INFORMATION).apply {
        headerText = "Readme"
        dialogPane.content = ScrollPane(Label(this@ParentComponent.javaClass.getModuleResource("Readme.txt").readText()))
    }.showAndWait()

}