package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextField
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import java.time.LocalDate

/**
 * Displays the Fix Month tool dialog
 */
fun ShowFixMonthDialog() =
    Stage().run {
        title = "Fix month tool"
        scene = Scene(FXMLLoader(Resources.getLayout("fix_month")).load())
        scene.stylize()
        initModality(Modality.APPLICATION_MODAL)

        showAndWait()

        // return
        scene.window.userData as? TimeEntry
    }


/**
 * The fix month controller
 */
class FixMonthController {

    /* ------------------------- nodes ------------------------- */

    @FXML
    lateinit var comment: TextField

    @FXML
    lateinit var issue: ChoiceBox<Issue>

    @FXML
    lateinit var futureDays: CheckBox

    @FXML
    lateinit var selectedWeek: CheckBox

    private val window get() = comment.scene.window // window

    /* ------------------------- functions ------------------------- */

    @FXML
    fun initialize() {
        // get issues
        val issues = AppController.runForeground { it.loadedIssues }

        if (issues == null) {
            // not loaded yet
            Alert(Alert.AlertType.ERROR, "Redmine is not loaded").apply {
                stylize()
            }.showAndWait()
            return
        }

        // assign issues
        issue.items += issues.sortedByDescending { it.spent }
        issue.selectionModel.select(0)

        // disable week if no day is selected
        if (AppController.runForeground { it.date } == null) {
            selectedWeek.enabled = false
        }

    }

    @FXML
    fun run() = AppController.runBackground({ model ->
        // get entries
        val entries = model.monthEntries ?: return@runBackground

        // get days of week or month
        // note: for week, this may 'fix' not loaded dates, but theoretically they should be empty
        (if (selectedWeek.isSelected) model.date?.weekDays() ?: return@runBackground else model.month.days())
            // excluding future (unless enabled)
            .filter { futureDays.isSelected || it <= LocalDate.now() }
            .forEach { day ->
                // get data of that day
                val dayEntries = entries.filter { it.spent_on == day }
                val expected = day.expectedHours
                val spent = dayEntries.sumOf { it.spent }
                if (expected - spent > 0) {
                    // pending hours, create entry
                    model.createTimeEntry(
                        issue = issue.selectionModel.selectedItem,
                        comment = comment.text,
                        spent = expected - spent,
                        date = day
                    )
                } else if (expected - spent < 0) {
                    // extra hours, remove time
                    dayEntries.forEach {
                        it.changeSpent(it.spent / spent * expected)
                        model.registerExternalChange(ChangeEvent.EntryContent)
                        model.registerExternalChange(ChangeEvent.DayHours)
                        model.registerExternalChange(ChangeEvent.MonthHours)
                    }
                }
            }
    }) {
        // when correctly finishes, exit
        if (it) cancel()
    }


    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

}

