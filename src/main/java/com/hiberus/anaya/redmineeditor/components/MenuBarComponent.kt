package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.dialogs.AppController
import com.hiberus.anaya.redmineeditor.dialogs.BatchEditor
import com.hiberus.anaya.redmineeditor.dialogs.DisplayEvidences
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
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
            dialogPane.content = ScrollPane(Label(Resources.getFile("Readme.txt").readText()))
                .apply { maxWidth = 100.0; maxHeight = 50.0 }
            stylize()
        }.showAndWait()
    }

    @FXML
    fun about() {
        Alert(Alert.AlertType.INFORMATION).apply {
            headerText = "About"
            contentText = "App made by Abel Naya"
            stylize()
            clearButtons()
            addButton(ButtonType("Source code")) {
                openInBrowser("https://gitlabdes.hiberus.com/anaya/redmineeditor")
            }
            addButton(ButtonType.CLOSE)
        }.showAndWait()
    }

    @FXML
    fun settings() = AppController.showSettings()

    @FXML
    fun hours() = OpenSpecialDaysFile()

    @FXML
    fun evidences() = DisplayEvidences()

    @FXML
    fun batchEditor() = BatchEditor()

}