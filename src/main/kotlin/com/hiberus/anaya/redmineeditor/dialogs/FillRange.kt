package com.hiberus.anaya.redmineeditor.dialogs

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineeditor.ResourceLayout
import com.hiberus.anaya.redmineeditor.commandline.Command
import com.hiberus.anaya.redmineeditor.model.AppController
import com.hiberus.anaya.redmineeditor.model.ChangeEvent
import com.hiberus.anaya.redmineeditor.model.Model
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.application.Application
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
    Stage().apply {
        title = "Fix month tool"
        scene = Scene(FXMLLoader(ResourceLayout("fix_month")).load())
        scene.stylize()
        centerInMouseScreen()
        initModality(Modality.APPLICATION_MODAL)

    }.showAndWait()


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
            selection.text = getSelectionRange(AppController.runForeground { it }, selectedWeek.isSelected, futureDays.isSelected, relative.isSelected)
                .let { if (it == null) "No days with selected properties" else "Run from ${it.fromDate} to ${it.toDate}, both inclusive" }
        }
        selectedWeek.selectedProperty().addListener(function)
        futureDays.selectedProperty().addListener(function)
        relative.selectedProperty().addListener(function)
        function(null, null, null)
    }

    @FXML
    fun run() = AppController.runBackground({
        FillRangeTool(it, listOf(issue.selectionModel.selectedItem to comment.text), getSelectionRange(it, selectedWeek.isSelected, futureDays.isSelected))
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

/**
 * Run this tool as a command line
 */
class FillRangeToolCommand : Command {
    override val name = "Command line variant of the FillRange tool"
    override val argument = "-fill"
    override val parameters = "[-test] [-fromDate=~0] [-toDate=~0] --issue=123 [--comment=\"A comment\"]"
    override val help = listOf(
        "-test, if specified, nothing will be uploaded (but changes that would have happened will be logged).",
        "fromDate, first day (inclusive) to fill. Either a date in format '2020/12/31' where each element can be a fixed value or an offset ('1', '+1' or '-1') with an optional day of week (mon,tue,wed,thu,fri,sat,sun) extension (+0/+0/+0/mon). The day can also be 'end' meaning 'end of month' (+0/+0/end). Today (+0/+0/+0) by default",
        "toDate, first day (inclusive) to fill. Either a date in format '2020/12/31' where each element can be a fixed value or an offset ('1', '+1' or '-1') with an optional day of week (mon,tue,wed,thu,fri,sat,sun) extension (+0/+0/+0/mon). The day can also be 'end' meaning 'end of month' (+0/+0/end). Today (+0/+0/+0) by default",
        "--issue=123 will create new entries assigned to the issue with id 123. You can specify multiple issues separating them by commas (--issue=123,456,789). In that case the missing hours will be split between them.",
        "--comment=\"A comment\" will create new entries with the comment 'A comment'. If omitted, an empty message will be used. For multiple issues you can override the comment of a specific one with --comment123=\"Specific issue\"",
        "Common usage: ./RedmineEditor(.bat) -fix -week -relative --issue=123 --comment=\"development\"",
        "On linux, add and adapt this to your cron for automatic imputation: 0 15 * * * ~/RedmineEditor -fix -week -relative --issue=123 --comment=\"development\" >> ~/logs/cron/imputation 2>&1",
        "On windows, create a bat with the command (RedmineEditor.bat -fix -week -relative --issue=123 --comment=\"development\") and create a Basic Task on the Task Scheduler to run it",
    )

    override fun run(parameters: Application.Parameters) {

        // read parameters
        val issueIds = parameters.named["issue"]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { it.toIntOrNull() }
            ?.distinct()
            ?: listOf() // list of ids separated by comma
        if (issueIds.isEmpty() && "issue" in parameters.named) println(parameters.named["issue"] + " is not an integer or list of integers")
        val test = ("-test" in parameters.unnamed).ifOK { println("> Testing mode, no changes will apply") }

        val fromDate = runCatching { parameters.named["fromDate"]?.parseCustomDate() ?: LocalDate.now() }.getOrElse {
            println("the fromDate parameter is invalid. Refer to the documentation for the exact format required.")
            return
        }
        val toDate = runCatching { parameters.named["toDate"]?.parseCustomDate() ?: LocalDate.now() }.getOrElse {
            println("the toDate parameter is invalid. Refer to the documentation for the exact format required.")
            return
        }
        println("Filling range: [$fromDate, $toDate]")

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

/**
 * Returns the selected range
 */
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


class LocalDateRange(val fromDate: LocalDate, val toDate: LocalDate)

/**
 * Returns all days between the two local dates (inclusive).
 * If null, or start > end, returns empty list
 */
private val LocalDateRange?.days: List<LocalDate>
    get() {
        // check invalid
        if (this == null) return emptyList()
        if (fromDate > toDate) return emptyList()
        // generate
        return mutableListOf(fromDate).apply {
            while (last() < toDate) add(last().plusDays(1))
        }
    }

private fun String.parseCustomDate(): LocalDate? {
    var date = LocalDate.now()
    val parts = split("/")
    if (parts.size < 3 || parts.size > 4) {
        println("Invalid date $this")
        return null
    }
    parts[0].let {
        date = when {
            it.startsWith("+") -> date.plusYears(it.removePrefix("+").toLong())
            it.startsWith("-") -> date.minusYears(it.removePrefix("-").toLong())
            else -> date.withYear(it.toInt())
        }
    }
    parts[1].let {
        date = when {
            it.startsWith("+") -> date.plusMonths(it.removePrefix("+").toLong())
            it.startsWith("-") -> date.minusMonths(it.removePrefix("-").toLong())
            else -> date.withMonth(it.toInt())
        }
    }
    parts[2].let {
        date = when {
            it == "end" -> date.atEndOfMonth()
            it.startsWith("+") -> date.plusDays(it.removePrefix("+").toLong())
            it.startsWith("-") -> date.minusDays(it.removePrefix("-").toLong())
            else -> date.withDayOfMonth(it.toInt())
        }
    }
    parts.getOrNull(3)?.let {
        date = date.with(ChronoField.DAY_OF_WEEK, listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun").indexOf(it).toLong())
    }

    return date
}