package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.dialogs.ShowBatchEditorDialog
import com.hiberus.anaya.redmineeditor.dialogs.ShowEvidencesDialog
import com.hiberus.anaya.redmineeditor.dialogs.ShowFixMonthDialog
import com.hiberus.anaya.redmineeditor.dialogs.ShowSettingsDialog
import com.hiberus.anaya.redmineeditor.utils.OpenSpecialDaysFile
import com.hiberus.anaya.redmineeditor.utils.stylize
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
    fun settings() = ShowSettingsDialog()

    @FXML
    fun hours() = OpenSpecialDaysFile()

    @FXML
    fun evidences() = ShowEvidencesDialog()

    @FXML
    fun batchEditor() = ShowBatchEditorDialog()

    @FXML
    fun fixMonth() = ShowFixMonthDialog()

}