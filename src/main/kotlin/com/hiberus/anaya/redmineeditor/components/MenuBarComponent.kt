package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.dialogs.*
import com.hiberus.anaya.redmineeditor.model.OpenColorsFile
import com.hiberus.anaya.redmineeditor.utils.OpenCalendarFile
import javafx.fxml.FXML

/** the top menubar with actions */
internal class MenuBarComponent {

    /* ------------------------- actions ------------------------- */

    @FXML
    fun readme() = ShowReadme()

    @FXML
    fun about() = ShowAbout()

    @FXML
    fun settings() = ShowSettingsDialog()

    @FXML
    fun hours() = OpenCalendarFile()

    @FXML
    fun colors() = OpenColorsFile()

    @FXML
    fun evidences() = ShowEvidencesDialog()

    @FXML
    fun batchEditor() = ShowBatchEditorDialog()

    @FXML
    fun fillRange() = ShowFillRangeDialog()

    @FXML
    fun calendarStatistics() = ShowCalendarStatisticsDialog()

}
