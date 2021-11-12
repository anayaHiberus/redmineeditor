package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.Redmine
import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.controller.MyException
import com.hiberus.anaya.redmineeditor.controller.SETTING
import com.hiberus.anaya.redmineeditor.controller.convert
import com.hiberus.anaya.redmineeditor.controller.value
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/* ------------------------- settings ------------------------- */

/**
 * set to true to auto-download today issues
 */
private val autoLoadTotalHours get() = SETTING.AUTO_LOAD_TOTAL_HOURS.value.toBoolean()

/**
 * Number of days for 'past' computations
 */
private val prevDays get() = runCatching { SETTING.PREV_DAYS.value.toLong().coerceIn(0, 28) }.getOrDefault(0)

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
     * iff there is at least something that was modified (and should be uploaded), null if not loaded
     */
    val hasChanges get() = redmine?.hasChanges

    /* ------------------------- setters ------------------------- */

    class Editor : Model() {

        /* ------------------------- changes ------------------------- */

        /**
         * list of changes
         */
        private val changes = mutableSetOf<ChangeEvents>()

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
        fun registerExternalChange(event: ChangeEvents) = changes.add(event)

        /* ------------------------- public setters ------------------------- */

        override var isLoading = true
            set(value) {
                field = value
                changes += ChangeEvents.Loading
            }

        override var month: YearMonth = YearMonth.of(1997, 1) // this should never be used
            set(value) {
                field = value
                changes += ChangeEvents.Month
            }

        override var day: Int? = null
            @Throws(MyException::class)
            set(value) {
                field = value?.takeIf { month.isValidDay(it) }
                prepareDay() // prepare day
                changes += ChangeEvents.Day // notify
            }

        /**
         * Sets the date to now
         */
        fun toNow() {
            day = LocalDate.now().dayOfMonth
            month = YearMonth.now()
        }

        /**
         * Loads the current month (if it is already loaded this does nothing)
         * Long operation
         *
         * @throws MyException on error
         */
        @Throws(MyException::class)
        fun loadDate() {
            try {
                // download (skip if not loaded)
                (redmine ?: return).downloadEntriesFromMonth(month).let { (newEntries, newIssues) ->
                    // and notify
                    if (newEntries) changes += ChangeEvents.EntryList
                    if (newIssues) changes += ChangeEvents.IssueList
                }
            } catch (e: IOException) {
                throw MyException("Network error", "Can't load content from Redmine. Try again later.", e)
            } catch (e: JSONException) {
                throw MyException("Parsing error", "Unknown Redmine response. Try again later.", e)
            }

            // prepare day
            prepareDay()
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
         * Creates a new Time Entry for [issue] on the current date with already [spent] hours and [comment], returns whether it was added or not
         *
         * @param issue for this issue
         */
        fun createTimeEntry(issue: Issue, spent: Double = 0.0, comment: String = ""): Boolean {
            val redmine = redmine ?: return false // skip if no api
            val date = date ?: return false // skip if no date

            redmine.createTimeEntry(issue = issue, spent_on = date, spent = spent, comment = comment).also {
                // autoload if required, ignore errors
                if (autoLoadTotalHours) {
                    // TODO: move the autoloading to Redmine object
                    runCatching { it.issue.downloadSpent() }.onFailure {
                        // warning
                        System.err.println("Error when loading spent, ignoring: $it")
                    }
                }
            }
            changes += ChangeEvents.EntryList
            return true
        }

        /**
         * Copies an existing entry to today, returns true if it was added
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
                if (redmine.downloadIssues(ids)) changes += ChangeEvents.IssueList

                // create entries
                val missingIds = ids.filter { id ->
                    // create new entries for existing ids, and keep missing ids
                    redmine.loadedIssues.firstOrNull { it.id == id }?.also { createTimeEntry(it) } == null
                }

                when {
                    // missing single issue
                    missingIds.size == 1 ->
                        throw MyException("Unknown issue", "The issue #${missingIds[0]} was not found or couldn't be loaded", warning = true)
                    // missing multiple issues
                    missingIds.size >= 2 ->
                        throw MyException("Unknown issues", "The issues ${missingIds.joinToString(", ") { "#$it" }} were not found or couldn't be loaded", warning = true)
                }
            } catch (e: IOException) {
                throw MyException("Error loading issues", "Can't load issues", e)
            }
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
                .map {
                    createTimeEntry(issue = it.issue, comment = it.comment)
                }

            // add missing assigned issues for today
            val todayIssues = (redmine.getEntriesForDate(date) ?: return).map { it.issue }.distinct() // temp variable
            // get issues assigned to us
            redmine.getAssignedIssues()
                // not in today
                .filterNot { it in todayIssues }
                // and create empty entries
                .map { createTimeEntry(issue = it) }

            // download all issues of today if configured
            if (autoLoadTotalHours) {
                // load all issues of today
                (redmine.getEntriesForDate(date) ?: return).map { it.issue }.distinct().forEach {
                    runCatching { it.downloadSpent() }.onFailure {
                        // warning
                        System.err.println("Error when loading spent, ignoring: $it")
                    }
                }
            }
        }

        /**
         * Clears and initializes (unless [clearOnly] is true) the redmine data
         */
        fun reloadRedmine(clearOnly: Boolean = false) {
            redmine = if (clearOnly) null else Redmine(SETTING.URL.value, SETTING.KEY.value, SETTING.READ_ONLY.value.toBoolean(), prevDays)
            changes += setOf(ChangeEvents.IssueList, ChangeEvents.EntryList, ChangeEvents.DayHours, ChangeEvents.Month) // all the hours of the month change TODO add a monthHours event
        }
    }

}