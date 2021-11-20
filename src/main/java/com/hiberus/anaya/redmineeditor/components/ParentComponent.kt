package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvents
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator


/**
 * Component for the parent app. Manages the loading indicator
 */
internal class ParentComponent {

    /* ------------------------- elements ------------------------- */

    @FXML
    lateinit var progress: ProgressIndicator  // that circular thingy

    @FXML
    lateinit var parent: Node // the main app, disabled while loading

    /* ------------------------- init ------------------------- */

    @FXML
    fun initialize() {
        // When loading, the indicator is shown and the whole app is disabled.
        AppController.onChanges(setOf(ChangeEvents.Loading)) {
            progress.isVisible = it.isLoading
            parent.isDisable = it.isLoading
        }

        // when app starts, reload
        AppController.reload(askIfChanges = false, resetDay = true)
    }

}