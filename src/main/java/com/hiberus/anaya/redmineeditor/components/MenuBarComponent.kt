package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.controller.AppController
import com.hiberus.anaya.redmineeditor.settings.SettingsController
import com.hiberus.anaya.redmineeditor.summary.DisplaySummary
import com.hiberus.anaya.redmineeditor.utils.getModuleResource
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
            dialogPane.content = ScrollPane(Label(this@MenuBarComponent.javaClass.getModuleResource("Readme.txt").readText()).apply { isWrapText = true })
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
    fun settings() {
        if (SettingsController.show())
            AppController.reload(reloadConfig = true)
    }

    @FXML
    fun summary() {
        DisplaySummary()
    }

}