package com.hiberus.anaya.redmineeditor.components

import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.dialogs.AppController
import com.hiberus.anaya.redmineeditor.dialogs.BatchEditor
import com.hiberus.anaya.redmineeditor.dialogs.DisplayEvidences
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.fxml.FXML
import javafx.scene.control.*
import java.time.LocalDate

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

    @FXML
    fun fixMonth() {
        ChoiceDialog(null, AppController.runForeground { it.loadedIssues })
            .apply {
                contentText = """
                    With this tool you can 'fix' the whole selected month (past days only) in just a few seconds:
                    - Any day where spent hours is the same (=) as the expected will not be changed
                    - Any day where spent hours is greater (>) than expected, all its entries will be reduced equally (If you should have spent 8 hours but you spent [2, 6, 2] = 10 then 2 hours will be reduced accordingly -> [1.6, 4.8, 1.6] = 8
                    - On any day where spent hours is less (<) than expected, a new entry for the selected issue with the selected comment will be created with the required hours
                     
                     After the tool runs you need to press upload to save the changes. In other words, you can run this tool and then reload without modifying anything, for testing.
                """.trimMargin()
            }
            .showAndWait().ifPresent { issue ->
                AppController.runBackground { model ->
                    val entries = model.monthEntries ?: return@runBackground

                    model.month.days()
                        .filter { it <= LocalDate.now() }
                        .forEach { day ->
                            val dayEntries = entries.filter { it.spent_on == day }
                            val expected = day.expectedHours
                            val spent = dayEntries.sumOf { it.spent }
                            if (expected - spent > 0) {
                                // create entry
                                model.createTimeEntry(issue = issue, spent = expected - spent, date = day)
                            } else if (expected - spent < 0) {
                                // remove extra time
                                dayEntries.forEach {
                                    it.changeSpent(it.spent / spent * expected)
                                    model.registerExternalChange(ChangeEvent.EntryContent)
                                    model.registerExternalChange(ChangeEvent.DayHours)
                                    model.registerExternalChange(ChangeEvent.MonthHours)
                                }
                            }

                        }
                }
            }
    }

}