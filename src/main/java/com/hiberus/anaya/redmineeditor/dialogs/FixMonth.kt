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
    fun run() = FixMonthTool(issue.selectionModel.selectedItem, comment.text, selectedWeek.isSelected, futureDays.isSelected) {
        // when correctly finishes, exit
        if (it) cancel()
    }


    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

}

/**
 * Fixes the selected month or [selectedWeek] optionally skipping [futureDays] adding new entries if required to an [issue] with a [comment]
 */
fun FixMonthTool(issue: Issue, comment: String = "", selectedWeek: Boolean = false, futureDays: Boolean = false, later: (Boolean) -> Unit) = AppController.runBackground({ model ->
    // get days of week or month
    (if (selectedWeek) model.date?.weekDays() ?: return@runBackground else model.month.days())
        // excluding future (unless enabled)
        .filter { futureDays || it <= LocalDate.now() }
        .forEach { day ->
            // get data of that day
            val dayEntries = model.getEntriesFromDate(day)
            val expected = day.expectedHours
            val spent = dayEntries.sumOf { it.spent }
            if (expected - spent > 0) {
                // pending hours, create entry
                val entry = model.createTimeEntry(
                    issue = issue,
                    comment = comment,
                    spent = expected - spent,
                    date = day
                )
                println(
                    if (entry != null) "Created entry: $entry"
                    else "Unable to create entry for $day"
                )
            } else if (expected - spent < 0) {
                // extra hours, remove time
                dayEntries.forEach {
                    it.changeSpent(it.spent / spent * expected)
                    model.registerExternalChange(ChangeEvent.EntryContent)
                    model.registerExternalChange(ChangeEvent.DayHours)
                    model.registerExternalChange(ChangeEvent.MonthHours)
                    println("Updated entry: $it")
                }
            }
        }
}, later)