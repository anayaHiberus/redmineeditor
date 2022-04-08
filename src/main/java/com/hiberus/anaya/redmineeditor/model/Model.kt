package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.READ_ONLY
import com.hiberus.anaya.redmineapi.Redmine
import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.dialogs.MyException
import com.hiberus.anaya.redmineeditor.dialogs.convert
import com.hiberus.anaya.redmineeditor.utils.errorln
import com.hiberus.anaya.redmineeditor.utils.expectedHours
import com.hiberus.anaya.redmineeditor.utils.ifOK
import com.hiberus.anaya.redmineeditor.utils.yearMonth
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/* ------------------------- settings ------------------------- */

/**
 * set to true to auto-download today issues
 */
private val autoLoadTotalHours get() = AppSettings.AUTO_LOAD_TOTAL_HOURS.value.toBoolean()

/**
 * Number of days for 'past' computations
 */
private val prevDays get() = runCatching { AppSettings.PREV_DAYS.value.toLong().coerceIn(0, 28) }.getOrDefault(0)

/* ------------------------- model ------------------------- */

/**
 * The data of the app
 */
abstract class Model {

    /* ------------------------- display data ------------------------- */

    /**
     * the loading state
     */
    abstract var isLoading: Boolean

    /**
     * the current month
     */
    abstract var month: YearMonth

    /**
     * the current day (null if there is no current day)
     */
    abstract var day: Int?

    /* ------------------------- redmine data ------------------------- */

    /**
     * interactions with the api. Initialized in [Editor.reloadRedmine]
     */
    protected var redmine: Redmine? = null

    /* ------------------------- compound data ------------------------- */

    /**
     * the current date: [month]+[day] (null if there is no current day)
     */
    val date
        get() = day?.let { month.atDay(it) }

    /**
     * calculates the hours spent in a [date], null if the date is not loaded
     */
    fun getSpent(date: LocalDate) =
        redmine?.getEntriesForDate(date)?.sumOf { it.spent }

    /**
     * calculates the hours spent in a [month], null if the month is not loaded
     */
    fun getSpent(month: YearMonth) =
        redmine?.getEntriesForMonth(month)?.sumOf { it.spent }

    /**
     * gets the pending hours from the current day (if negative, it means the user spent more than necessary)
     */
    fun getPending() =
        date?.run { getSpent(this)?.let { expectedHours - it } }

    /**
     * the entries that should be displayed on the current day (null if no current day or not loaded)
     */
    val dayEntries
        get() = date?.let { redmine?.getEntriesForDate(it) }

    /**
     * all distinct loaded issues (readonly), null if not loaded
     */
    val loadedIssues
        get() = redmine?.loadedIssues?.toSet()

    /**
     * Iff the assigned issues are already loaded
     */
    val loadedAssigned
        get() = redmine?.assignedLoaded

    /**
     * Return all entries from the current month
     */
    val monthEntries: List<TimeEntry>?
        get() = redmine?.getEntriesForMonth(month)

    /**
     * iff there is at least something that was modified (and should be uploaded), null if not loaded
     */
    val hasChanges get() = redmine?.hasChanges ?: false

    /* ------------------------- setters ------------------------- */

    class Editor : Model() {

        /* ------------------------- changes ------------------------- */

        /**
         * list of changes
         */
        private val changes = mutableSetOf<ChangeEvent>()

        /**
         * @return changes made to this model since last call (or initialization)
         */
        fun getChanges() =
            changes.toSet().also { changes.clear() }

        /**
         * Registers an external change.
         * TODO: somehow remove this
         *
         * @param event event to register
         */
        fun registerExternalChange(event: ChangeEvent) = changes.add(event)

        /* ------------------------- getters ------------------------- */

        val user: String?
            get() = redmine?.getUserName()

        /* ------------------------- public setters ------------------------- */

        override var isLoading = true
            set(value) {
                field = value
                changes += ChangeEvent.Loading
            }

        override var month: YearMonth = YearMonth.of(1997, 1) // this should never be used
            set(value) {
                field = value
                changes += ChangeEvent.Month
                loadMonth()
            }

        override var day: Int? = null
            @Throws(MyException::class)
            set(value) {
                field = value?.takeIf { month.isValidDay(it) }
                changes += ChangeEvent.Day // notify
                prepareDay() // prepare day
            }

        /**
         * Sets the date to now
         */
        fun toNow() {
            day = LocalDate.now().dayOfMonth
            month = YearMonth.now()
        }

        /**
         * Loads a month [current by default] (if it is already loaded this does nothing)
         * Long operation
         *
         * @throws MyException on error
         */
        @Throws(MyException::class)
        fun loadMonth(month: YearMonth? = null) {
            val month = month ?: this.month
            try {
                // download (skip if not loaded)
                (redmine ?: return).downloadEntriesFromMonth(month).let { (newEntries, newIssues, loaded) ->
                    // and notify
                    if (newEntries) changes += ChangeEvent.EntryList
                    if (newIssues) changes += ChangeEvent.DayIssues
                    if (loaded) changes += ChangeEvent.MonthHours
                }
            } catch (e: IOException) {
                throw MyException("Network error", "Can't load content from Redmine. Try again later.", e)
            } catch (e: JSONException) {
                throw MyException("Parsing error", "Unknown Redmine response. Try again later.", e)
            }

        }

        /**
         * Uploads all modified data
         * Long operation
         *
         * @throws MyException on error
         */
        @Throws(MyException::class)
        fun uploadAll() {
            redmine?.uploadAll()?.convert {
                // on upload error
                MyException("Updating error", "An error occurred while updating data, some changes were not saved")
            }?.let { throw it }
        }

        /**
         * Creates a new Time Entry for [issue] on [date], or the current day if null, with already [spent] hours and [comment], returns whether it was added or not
         */
        fun createTimeEntry(
            issue: Issue,
            spent: Double = 0.0,
            comment: String = "",
            date: LocalDate? = null,
            loadHours: Boolean = true
        ): TimeEntry? {
            val redmine = redmine ?: return null // skip if no api
            val spent_on = date ?: this.date ?: return null // skip if no date

            val entry = redmine.createTimeEntry(issue = issue, spent_on = spent_on, spent = spent, comment = comment).also {
                // autoload if required, ignore errors
                if (autoLoadTotalHours && loadHours) {
                    // TODO: move the autoloading to Redmine object
                    runCatching { it.issue.downloadExtra().ifOK { changes += ChangeEvent.IssueContent } }.onFailure {
                        // warning
                        errorln("Error when loading spent, ignoring: $it")
                    }
                }
            }
            changes += ChangeEvent.EntryList
            if (spent > 0 && spent_on == this.date) changes += ChangeEvent.DayHours
            if (spent > 0 && spent_on.yearMonth == month) changes += ChangeEvent.MonthHours
            return entry
        }

        /**
         * Copies an existing entry to today, returns it
         */
        fun copyTimeEntry(entry: TimeEntry) = createTimeEntry(entry.issue, entry.spent, entry.comment)

        /**
         * Creates multiple new time entries for current date (does nothing if there is no current day)
         *
         * @param ids each one with an id from this
         */
        @Throws(MyException::class)
        fun createTimeEntries(ids: List<Int>) {
            val redmine = redmine ?: return // skip if no api
            date ?: return // skip if no date

            try {
                // download missing issues, if any
                if (redmine.downloadIssues(ids)) changes += ChangeEvent.DayIssues

                // create entries
                val missingIds = ids.filter { id ->
                    // create new entries for existing ids, and keep missing ids
                    redmine.loadedIssues.firstOrNull { it.id == id }?.also { createTimeEntry(it) } == null
                }

                when {
                    // missing single issue
                    missingIds.size == 1 ->
                        throw MyException(
                            "Unknown issue",
                            "The issue #${missingIds[0]} was not found or couldn't be loaded",
                            warning = true
                        )
                    // missing multiple issues
                    missingIds.size >= 2 ->
                        throw MyException(
                            "Unknown issues",
                            "The issues ${missingIds.joinToString(", ") { "#$it" }} were not found or couldn't be loaded",
                            warning = true
                        )
                }
            } catch (e: IOException) {
                throw MyException("Error loading issues", "Can't load issues", e)
            }
        }

        /**
         * Load issues with [issuesIds]
         */
        fun loadIssues(issuesIds: List<Int>) {
            if (redmine?.downloadIssues(issuesIds) == true) changes += ChangeEvent.DayIssues
        }

        /**
         * Loads assigned issues
         */
        fun loadAssigned(autoCreate: Boolean = false) {
            val redmine = redmine ?: return

            // load
            val assignedIssues = redmine.getAssignedIssues()
            changes += ChangeEvent.Assigned

            // stop if no need to create
            if (!autoCreate) return

            // add missing assigned issues for today
            val date = date ?: return
            val todayIssues = (redmine.getEntriesForDate(date) ?: return).map { it.issue }.distinct() // temp variable

            // get issues assigned to us
            assignedIssues
                // not in today
                .filterNot { it in todayIssues }
                // and create empty entries
                .forEach { createTimeEntry(issue = it, loadHours = false) }

        }

        /**
         * Return the entries of a specified date (loads them if necessary)
         */
        fun getEntriesFromDate(date: LocalDate) = run {
            loadMonth(date.yearMonth)
            redmine?.getEntriesForDate(date) ?: emptyList()
        }

        /* ------------------------- private setters ------------------------- */

        /**
         * prepares the current day
         */
        @Throws(MyException::class)
        private fun prepareDay() {
            val redmine = redmine ?: return // skip if no api
            val date = date ?: return // skip if no date

            // add copy of past issues from previous days
            // for all entries in previous days (sorted by date)
            (0L..prevDays).flatMap { redmine.getEntriesForDate(date.minusDays(it)) ?: return } // skip if not loaded yet
                // keep one for each issue
                .distinctBy { it.issue }
                // then remove those from today
                .filterNot { it.wasSpentOn(date) }
                // and create entry
                .forEach {
                    createTimeEntry(issue = it.issue, comment = it.comment, loadHours = false)
                }

            AppController.fireChanges()

            // download assigned if required
            if (AppSettings.AUTO_LOAD_ASSIGNED.value.toBoolean()) loadAssigned(autoCreate = true)

            AppController.fireChanges()

            // download all issues of today if configured
            if (autoLoadTotalHours) {
                // load all issues of today
                (redmine.getEntriesForDate(date) ?: return).map { it.issue }.distinct().forEach {
                    runCatching { it.downloadExtra().ifOK { changes += ChangeEvent.IssueContent } }.onFailure {
                        // warning
                        errorln("Error when loading spent, ignoring: $it")
                    }
                }
            }
        }

        /**
         * Clears and initializes (unless [clearOnly] is true) the redmine data
         */
        fun reloadRedmine(clearOnly: Boolean = false) {
            redmine = if (clearOnly) null else Redmine(AppSettings.URL.value, AppSettings.KEY.value, prevDays)
            READ_ONLY = AppSettings.READ_ONLY.value.toBoolean()
            changes += setOf(
                ChangeEvent.DayIssues,
                ChangeEvent.EntryList,
                ChangeEvent.DayHours,
                ChangeEvent.MonthHours,
                ChangeEvent.Assigned
            )

            // reload data
            AppController.fireChanges()
            loadMonth()
            AppController.fireChanges()
            prepareDay()
        }
    }

}