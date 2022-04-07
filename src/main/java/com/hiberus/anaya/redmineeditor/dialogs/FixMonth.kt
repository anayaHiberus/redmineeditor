package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.Resources
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Application
import javafx.application.Platform
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
import java.util.concurrent.CountDownLatch

/* ------------------------- window ------------------------- */

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
    fun run() = AppController.runBackground({
        FixMonthTool(it, issue.selectionModel.selectedItem, comment.text, selectedWeek.isSelected, futureDays.isSelected)
    }) {
        // when correctly finishes, exit
        if (it) cancel()
    }


    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

}

/* ------------------------- command line ------------------------- */

/**
 * Runs the fixRunTool from the command line
 */
fun FixRunToolCommandLine(parameters: Application.Parameters) {
    println(" === Fix run tool ===")

    // read parameters
    val issueId = parameters.named["issue"]?.toIntOrNull() ?: run {
        // issue is mandatory
        System.err.println(if ("issue" in parameters.named) parameters.named["issue"] + " is not an integer" else "Missing issue parameter")
        Platform.exit()
        return
    }
    val comment = parameters.named["comment"] ?: "".also { println("No comment provided, an empty string will be used") }
    val week = ("-week" in parameters.unnamed).also { println("Fixing " + if (it) "week" else "month") }
    val future = ("-future" in parameters.unnamed).also { println("Fixing " + if (it) "future days too" else "past days only") }
    val test = ("-test" in parameters.unnamed).also { if (it) println("Testing mode, no changes will apply") }

    // start process
    val latch = CountDownLatch(1)
    var step = 1
    AppController.onChanges(setOf(ChangeEvent.Loading)) { readOnlyModel ->
        if (readOnlyModel.isLoading) return@onChanges
        when (step) {
            // first initialize (load)
            1 -> {
                println("Initializing")
                AppController.reload(askIfChanges = false, resetDay = true)
            }
            // now load issue
            2 -> {
                println("Loading issue #$issueId")
                AppController.runBackground { it.loadIssues(listOf(issueId)) }
            }
            // run tool
            3 -> readOnlyModel.loadedIssues
                ?.find { it.id == issueId }
                ?.also { println("Loaded: $it") }
                ?.let { issue ->
                    println("Running:")
                    AppController.runBackground { model ->
                        FixMonthTool(model, issue, comment, week, future, test).onEach { println("    $it") }
                    }
                }
                ?: run {
                    // no issue, exit
                    System.err.println("No issue with id $issueId was found, exiting")
                    latch.countDown()
                }
            // upload
            4 -> AppController.runBackground {
                if (!test) {
                    println("Uploading...")
                    it.uploadAll()
                }
            }
            // notify
            5 -> {
                println(if (test) "Testing completed, no changes uploaded" else "Uploaded changes")
                latch.countDown()
            }
        }
        step++
    }

    AppController.runBackground { /* start process */ }
    latch.await()
}

/* ------------------------- tool ------------------------- */

/**
 * Fixes the selected month or [selectedWeek] optionally skipping [futureDays] adding new entries if required to an [issue] with a [comment]
 * Returns the changes
 * if [test] is true, no changes will be made (only logged)
 */
private fun FixMonthTool(model: Model.Editor, issue: Issue, comment: String = "", selectedWeek: Boolean = false, futureDays: Boolean = false, test: Boolean = false) =
    // get days of week or month
    (if (selectedWeek) (model.date ?: LocalDate.now()).weekDays() else model.month.days())
        // excluding future (unless enabled)
        .filter { futureDays || it <= LocalDate.now() }
        .flatMap { day ->
            // get data of that day
            val dayEntries = model.getEntriesFromDate(day)
            val expected = day.expectedHours
            val spent = dayEntries.sumOf { it.spent }
            val pending = expected - spent
            if (pending > 0) {
                // pending hours, create entry
                if (!test) {
                    model.createTimeEntry(
                        issue = issue,
                        comment = comment,
                        spent = pending,
                        date = day
                    ) ?: return@flatMap listOf("[$day] ERROR: unable to create entry, is Redmine working?")
                }

                // return
                listOf("[$day] Created entry #${issue.id} ($comment) : -> ${pending.formatHours()}")
            } else if (pending < 0) {
                // extra hours, remove time
                dayEntries.map {
                    val oldSpent = it.spent
                    val newSpent = oldSpent / spent * expected
                    if (!test) {
                        it.changeSpent(newSpent)
                        model.registerExternalChange(ChangeEvent.EntryContent)
                        model.registerExternalChange(ChangeEvent.DayHours)
                        model.registerExternalChange(ChangeEvent.MonthHours)
                    }

                    // return
                    "[$day] Updated entry: #${it.issue.id} (${it.comment}) : ${oldSpent.formatHours()} -> ${newSpent.formatHours()}"
                }
            } else
            // ok day
                emptyList()
        }