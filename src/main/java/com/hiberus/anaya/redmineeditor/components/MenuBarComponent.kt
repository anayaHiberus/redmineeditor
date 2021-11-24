package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.evidences.DisplayEvidences
import com.hiberus.anaya.redmineeditor.utils.stylize
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane

/**
 * the top menubar with actions
 */
internal class MenuBarComponent {

    /* ------------------------- actions ------------------------- */

    @FXML
    fun readme() {
        Alert(Alert.AlertType.INFORMATION).apply {
            headerText = "Readme"
            dialogPane.content = ScrollPane(Label(Resources.getFile("Readme.txt").readText())).apply { maxWidth = 100.0; maxHeight = 50.0 }
            stylize()
        }.showAndWait()
    }

    @FXML
    fun about() {
        Alert(Alert.AlertType.INFORMATION).apply {
            headerText = "About"
            contentText = "App made by Abel Naya"
            stylize()
        }.showAndWait()
    }

    @FXML
    fun settings() = AppController.showSettings()

    @FXML
    fun evidences() {
        DisplayEvidences()
    }

}