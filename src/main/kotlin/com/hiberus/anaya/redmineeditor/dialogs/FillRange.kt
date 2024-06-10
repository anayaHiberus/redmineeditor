package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Application
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.WindowEvent
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.temporal.ChronoField
import java.util.concurrent.CountDownLatch

/* ------------------------- window ------------------------- */

/** Displays the Fill Range tool dialog */
fun ShowFillRangeDialog() =
    Stage().apply {
        title = "Fill range tool"
        scene = Scene(FXMLLoader(ResourceLayout("fill_range")).load())
        scene.stylize()
        centerInMouseScreen()
        initModality(Modality.APPLICATION_MODAL)

    }.showAndWait()


/** The fill range controller */
class FillRangeController {

    /* ------------------------- nodes ------------------------- */

    @FXML
    lateinit var presets: MenuButton

    @FXML
    lateinit var toDate: DatePicker

    @FXML
    lateinit var fromDate: DatePicker

    @FXML
    lateinit var comment: TextField

    @FXML
    lateinit var issue: ChoiceBox<Issue>

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

        // fill presets
        val currentDate = now()
        val currentMonth = currentDate.yearMonth
        val (selectedDate, selectedMonth) = AppController.runForeground { it.date to it.month }

        presets.items.addAll(
            MenuItem("last 30/31 days").apply { setOnAction { fromDate.value = currentDate.minusMonths(1); toDate.value = currentDate } },
            MenuItem("last 7 days").apply { setOnAction { fromDate.value = currentDate.minusDays(7); toDate.value = currentDate } },
        )

        if (currentMonth != selectedMonth) {
            // different selected/current months
            presets.items.addAll(
                MenuItem("full current month").apply { setOnAction { fromDate.value = currentMonth.atDay(1); toDate.value = currentMonth.atEndOfMonth() } },
                MenuItem("full selected month").apply { setOnAction { fromDate.value = selectedMonth.atDay(1); toDate.value = selectedMonth.atEndOfMonth() } },
            )
        } else {
            // same selected/current month
            presets.items.add(
                MenuItem("full current/selected month").apply { setOnAction { fromDate.value = currentMonth.atDay(1); toDate.value = currentMonth.atEndOfMonth() } }
            )
        }
        if (selectedDate == null) {
            // no selected date (only current)
            presets.items.add(
                MenuItem("full current week").apply { setOnAction { fromDate.value = currentDate.withDayOfWeek(MONDAY); toDate.value = currentDate.withDayOfWeek(SUNDAY) } },
            )
        } else if (selectedDate != currentDate) {
            // selected && different selected/current date
            presets.items.addAll(
                MenuItem("full current week").apply { setOnAction { fromDate.value = currentDate.withDayOfWeek(MONDAY); toDate.value = currentDate.withDayOfWeek(SUNDAY) } },
                MenuItem("full selected week").apply { setOnAction { fromDate.value = selectedDate.withDayOfWeek(MONDAY); toDate.value = selectedDate.withDayOfWeek(SUNDAY) } },
            )
        } else {
            // selected && same selected/current date
            presets.items.add(
                MenuItem("full current/selected week").apply { setOnAction { fromDate.value = currentDate.withDayOfWeek(MONDAY); toDate.value = currentDate.withDayOfWeek(SUNDAY) } }
            )
        }

    }

    @FXML
    fun run() = AppController.runBackground({
        FillRangeTool(it, listOf(issue.selectionModel.selectedItem to comment.text), LocalDateRange(fromDate.value, toDate.value))
    }) {
        // when correctly finishes, exit
        if (it) cancel()
    }


    @FXML
    // pressing cancel is the same as pressing the 'x' close button
    fun cancel() = window.fireEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST))

}

/* ------------------------- command line ------------------------- */

/** Old command */
class FixMonthToolCommand : Command {
    override val name = "[DEPRECATED: Use FillRangeTool] Command line variant of the old FixMonth tool."
    override val argument = "-fix"
    override val parameters = "[-test] [-week] [-future] [-relative] --issue=123 [--comment=\"A comment\"]"
    override val help = listOf(
        "DEPRECATED: Use the replacement FillRangeTool. Running this command will tell you the replacement command.",
        "-week, if specified, will run the tool on the current week only. If not specified, the tool wil run on the current month.",
        "-future, if specified, days after today will also be considered. If not specified, only past and today will be checked.",
        "-relative, if specified, the interval will be relative (past week/month). If not specified, interval will be absolute (current week/month). Recommended (in absolute mode, running this on day 1 or monday will not fix any past days).",
        "The rest of the parameters are explained in the FillRangeTool."
    )

    override fun run(parameters: Application.Parameters) {

        // read parameters
        val test = "-test" in parameters.unnamed
        val week = "-week" in parameters.unnamed
        val relative = "-relative" in parameters.unnamed
        val future = "-future" in parameters.unnamed

        val newParameters = listOfNotNull(
            "-fill",
            "-test".takeIf { test },
            "--fromDate=+0/${mapOf("truetrue" to "+0/-7", "truefalse" to "+0/+0/mon", "falsetrue" to "-1/+0", "falsefalse" to "+0/1")["$week$relative"]}",
            "--toDate=+0/${if (week) "+0/+0/sun" else "+0/end"}".takeIf { future && !relative },
            parameters.named["issue"]?.let { "--issue=$it" },
            *parameters.named.filterKeys { it.startsWith("comment") }.map { (key, value) -> "--$key=\"$value\"" }.toTypedArray()
        )
        println("Deprecated. Replace this command with the following: ./RedmineEditor " + newParameters.joinToString(" "))

        FillRangeToolCommand().run(SimpleParameters(newParameters.toTypedArray()))
    }

}

/** Run this tool as a command line */
class FillRangeToolCommand : Command {
    override val name = "Command line variant of the FillRange tool"
    override val argument = "-fill"
    override val parameters = "[-test] [-fromDate=+0/+0/+0] [-toDate=+0/+0/+0] --issue=123 [--comment=\"A comment\"]"
    override val help = listOf(
        "-test, if specified, nothing will be uploaded (but changes that would have happened will be logged).",
        "--fromDate, first day (inclusive) to fill. $CUSTOM_DATE_FORMAT_EXPLANATION. Today (+0/+0/+0) by default",
        "--toDate, last day (inclusive) to fill. $CUSTOM_DATE_FORMAT_EXPLANATION. Today (+0/+0/+0) by default",
        "--issue=123 will create new entries assigned to the issue with id 123. You can specify multiple issues separating them by commas (--issue=123,456,789). In that case the missing hours will be split between them.",
        "--comment=\"A comment\" will create new entries with the comment 'A comment'. If omitted, an empty message will be used. For multiple issues you can override the comment of a specific one with --comment123=\"Specific issue\"",
        "Common usage: ./RedmineEditor(.bat) -fill --fromDate=+0/+0/-7 --issue=123 --comment=\"development\"",
        "On linux, add and adapt this to your cron for automatic imputation: 0 15 * * * ~/RedmineEditor -fill --fromDate=+0/+0/-7 --issue=123 --comment=\"development\" >> ~/logs/cron/imputation 2>&1",
        "On windows, create a bat with the command (RedmineEditor.bat -fill --fromDate=+0/+0/-7 --issue=123 --comment=\"development\") and create a Basic Task on the Task Scheduler to run it",
    )

    override fun run(parameters: Application.Parameters) {

        // read parameters
        val test = ("-test" in parameters.unnamed).ifOK { println("> Testing mode, no changes will apply") }

        var errors = false
        val issueIds = parameters.named["issue"]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { it.toIntOrNull() }
            ?.distinct()
            ?.also {
                if (it.isEmpty()) {
                    errorln("the issue parameter (${parameters.named["issue"]}) is not an integer or list of integers")
                    errors = true
                }
            }
            ?: listOf() // list of ids separated by comma

        val fromDate = runCatching { parameters.named["fromDate"]?.parseCustomDateFormat() }.onFailure {
            errorln("the fromDate parameter (${parameters.named["fromDate"]}) is invalid. $CUSTOM_DATE_FORMAT_EXPLANATION.")
            errors = true
        }.getOrNull() ?: LocalDate.now()
        val toDate = runCatching { parameters.named["toDate"]?.parseCustomDateFormat() ?: LocalDate.now() }.onFailure {
            errorln("the toDate parameter (${parameters.named["toDate"]}) is invalid. $CUSTOM_DATE_FORMAT_EXPLANATION.")
            errors = true
        }.getOrNull() ?: LocalDate.now()

        if (errors) {
            println("Fix the errors and try again")
            return
        }

        println("> Filling range: [$fromDate, $toDate]")

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
                    println("Loading issues ${issueIds.joinToString(", ") { "#$it" }}")
                    AppController.runBackground { it.loadIssues(issueIds) }
                }
                // run tool
                3 -> issueIds.map { issueId ->
                    // convert issue id to issue object
                    readOnlyModel.loadedIssues
                        ?.find { it.id == issueId }
                        ?.also { println(it.toString().lines().joinToString("\n        ", "Loaded: ")) }
                        ?: run {
                            // no issue, exit
                            errorln("No issue with id $issueId was found, exiting")
                            latch.countDown()
                            return@onChanges
                        }
                }.map { issue ->
                    // attach comment for each issue
                    issue to (parameters.named["comment${issue.id}"] ?: parameters.named["comment"] ?: "".also { println("> No specific nor default comment provided for issue ${issue.id}, an empty string will be used") })
                }.let { issues ->
                    // run
                    println("Running:")
                    AppController.runBackground { model ->
                        FillRangeTool(model, issues, LocalDateRange(fromDate, toDate), test).onEach { println("    $it") }
                    }
                }
                // upload
                4 -> AppController.runBackground {
                    if (it.hasChanges) {
                        if (!test) {
                            println("Uploading...")
                            it.uploadAll()
                        } else println("Testing mode, changes have not being uploaded")
                    } else println("No changes")
                }
                // notify
                5 -> {
                    println(if (test) "Testing completed" else "Completed")
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
 * Fills a date [range] adding new entries if required to one or more [issues] with a specific comment each (if provided)
 * Returns the changes
 * if [test] is true, no changes will be made (only logged)
 */
fun FillRangeTool(model: Model.Editor, issues: List<Pair<Issue, String>>, range: LocalDateRange?, test: Boolean = false) =
    // get days of week or month
    range.days
        .flatMap parent@{ day ->
            // get data of that day
            val dayEntries = model.getEntriesFromDate(day)
            val expected = day.expectedHours
            val spent = dayEntries.sumOf { it.spent }
            val pending = expected - spent
            if (pending > 0) {
                // pending hours
                if (issues.isEmpty()) listOf("[$day] There are $pending pending hours, but no issues were specified, skipping")
                else {
                    // split between the issues
                    val pendingEach = pending / issues.size
                    issues.flatMap { (issue, comment) ->
                        // find a matching entry first
                        dayEntries.firstOrNull { it.issue == issue && it.comment == comment }
                            ?.let {
                                // existing entry with the same data, update
                                val oldSpent = it.spent
                                val newSpent = oldSpent + pendingEach
                                if (!test) {
                                    it.changeSpent(newSpent)
                                    model.registerExternalChange(ChangeEvent.EntryContent)
                                    model.registerExternalChange(ChangeEvent.DayHours)
                                    model.registerExternalChange(ChangeEvent.MonthHours)
                                }
                                listOf("[$day] Updated entry: #${issue.id} (${comment}) : ${oldSpent.formatHours()} -> ${newSpent.formatHours()}")
                            } ?: run {
                            // no entry with the data, create new one
                            if (!test) {
                                model.createTimeEntry(
                                    issue = issue,
                                    comment = comment,
                                    spent = pendingEach,
                                    date = day
                                ) ?: return@parent listOf("[$day] ERROR: unable to create entry, is Redmine working?")
                            }
                            listOf("[$day] Created entry #${issue.id} ($comment) : -> ${pendingEach.formatHours()}")
                        }
                    }
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

/** Returns the selected range */
private fun getSelectionRange(model: Model, selectedWeek: Boolean = false, futureDays: Boolean = false, relative: Boolean = false): LocalDateRange? {
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

    return LocalDateRange(start, end)
}


