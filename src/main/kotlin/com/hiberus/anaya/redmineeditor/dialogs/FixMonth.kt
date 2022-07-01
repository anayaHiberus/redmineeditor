package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.util.concurrent.CountDownLatch

/* ------------------------- window ------------------------- */

/**
 * Displays the Fix Month tool dialog
 */
fun ShowFixMonthDialog() =
    Stage().run {
        title = "Fix month tool"
        scene = Scene(FXMLLoader(ResourceLayout("fix_month")).load())
        scene.stylize()
        centerInMouseScreen()
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

    @FXML
    lateinit var relative: CheckBox

    @FXML
    lateinit var selection: Label

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

        // selected range
        val function: (observable: ObservableValue<out Boolean>?, oldValue: Boolean?, newValue: Boolean?) -> Unit = { _, _, _ ->
            selection.text = getSelectionRange(selectedWeek.isSelected, futureDays.isSelected, relative.isSelected, AppController.runForeground { it })
                .let { if (it == null) "No days with selected properties" else "Run from ${it.first} to ${it.second}, both inclusive" }
        }
        selectedWeek.selectedProperty().addListener(function)
        futureDays.selectedProperty().addListener(function)
        relative.selectedProperty().addListener(function)
        function(null, null, null)
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
 * Run this tool as a command line
 */
class FixMonthToolCommand : Command {
    override val name = "Command line variant of the FixMonth tool"
    override val argument = "-fix"
    override val parameters = "[-test] [-week] [-future] [-relative] --issue=123 [--comment=\"A comment\"]"
    override val help = listOf(
        "-test, if specified, nothing will be uploaded (but changes that would have happened will be logged).",
        "-week, if specified, will run the tool on the current week only. If not specified, the tool wil run on the current month.",
        "-future, if specified, days after today will also be considered. If not specified, only past and today will be checked.",
        "-relative, if specified, the interval will be relative (past week/month). If not specified, interval will be absolute (current week/month). Recommended (in absolute mode, running this on day 1 or monday will not fix any past days).",
        "--issue=123 will create new entries assigned to the issue with id 123. This parameter is mandatory.",
        "--comment=\"A comment\" will create new entries with the comment 'A comment'. If omitted, an empty message will be used.",
        "Common usage: ./RedmineEditor(.bat) -fix -week -relative --issue=123 --comment=\"development\"",
        "On linux, add and adapt this to your cron for automatic imputation: 0 15 * * * ~/RedmineEditor -fix -week -relative --issue=123 --comment=\"development\" >> ~/logs/cron/imputation 2>&1"
    )

    override fun run(parameters: Application.Parameters) {

        // read parameters
        val issueId = parameters.named["issue"]?.toIntOrNull() ?: run {
            // issue is mandatory
            errorln(if ("issue" in parameters.named) parameters.named["issue"] + " is not an integer" else "Missing issue parameter")
            Platform.exit()
            return
        }
        val comment = parameters.named["comment"] ?: "".also { println("> No comment provided, an empty string will be used") }
        val week = ("-week" in parameters.unnamed).also { println("> Fixing " + if (it) "week" else "month") }
        val relative = ("-relative" in parameters.unnamed).also { println("> Using " + (if (it) "relative" else "absolute") + " interval") }
        val future = ("-future" in parameters.unnamed).also { println("> Fixing " + if (it) "future days too" else "past days only") }
        val test = ("-test" in parameters.unnamed).ifOK { println("> Testing mode, no changes will apply") }

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
                            FixMonthTool(model, issue, comment, week, future, relative, test).onEach { println("    $it") }
                        }
                    }
                    ?: run {
                        // no issue, exit
                        errorln("No issue with id $issueId was found, exiting")
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

        // wait until everything ends
        latch.await()
    }

}

/* ------------------------- tool ------------------------- */

/**
 * Fixes the selected month or [selectedWeek] optionally skipping [futureDays] adding new entries if required to an [issue] with a [comment]
 * Returns the changes
 * if [test] is true, no changes will be made (only logged)
 */
private fun FixMonthTool(model: Model.Editor, issue: Issue, comment: String = "", selectedWeek: Boolean = false, futureDays: Boolean = false, relative: Boolean = false, test: Boolean = false) =
    // get days of week or month
    getSelectionRange(selectedWeek, futureDays, relative, model).days
        .flatMap { day ->
            // get data of that day
            val dayEntries = model.getEntriesFromDate(day)
            val expected = day.expectedHours
            val spent = dayEntries.sumOf { it.spent }
            val pending = expected - spent
            if (pending > 0) {
                // pending hours, find a matching entry first
                dayEntries.firstOrNull { it.issue == issue && it.comment == comment }
                    ?.let {
                        // existing entry with the same data, update
                        val oldSpent = it.spent
                        val newSpent = oldSpent + pending
                        if (!test) {
                            it.changeSpent(newSpent)
                        }
                        listOf("[$day] Updated entry: #${issue.id} (${comment}) : ${oldSpent.formatHours()} -> ${newSpent.formatHours()}")
                    } ?: run {
                    // no entry with the data, create new one
                    if (!test) {
                        model.createTimeEntry(
                            issue = issue,
                            comment = comment,
                            spent = pending,
                            date = day
                        ) ?: return@flatMap listOf("[$day] ERROR: unable to create entry, is Redmine working?")
                    }
                    listOf("[$day] Created entry #${issue.id} ($comment) : -> ${pending.formatHours()}")
                }
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
            } else {
                // ok day
                emptyList()
            }
        }

/**
 * Returns the selected range
 */
private fun getSelectionRange(selectedWeek: Boolean, futureDays: Boolean, relative: Boolean, model: Model): Pair<LocalDate, LocalDate>? {
    val now = LocalDate.now()

    // init
    val selectedDay = model.date ?: now
    val selectedMonth = model.month

    val start =
        if (selectedWeek)
            if (relative) selectedDay.minusWeeks(1) // relative week: 7 days in the past
            else selectedDay.with(ChronoField.DAY_OF_WEEK, 1) // absolute week: start of current week
        else
            if (relative) selectedDay.minusMonths(1) // relative month: 1 month in the past
            else selectedMonth.atDay(1) // absolute month: start of current month

    var end =
        if (selectedWeek)
            if (relative) selectedDay // relative week: today
            else selectedDay.with(ChronoField.DAY_OF_WEEK, 7) // absolute week: end of current week
        else
            if (relative) selectedDay // relative month: today
            else selectedMonth.atEndOfMonth() // absolute month: end of current month

    if (!futureDays) {
        // remove future
        if (start > now) return null
        if (end > now) end = now
    }

    return start to end
}

/**
 * Returns all days between two local dates (inclusive).
 * If null or end < start, returns empty list
 */
private val Pair<LocalDate, LocalDate>?.days: List<LocalDate>
    get() {
        // check invalid
        if (this == null) return emptyList()
        if (first > second) return emptyList()
        // generate
        return mutableListOf(first).apply {
            while (last() < second) add(last().plusDays(1))
        }
    }