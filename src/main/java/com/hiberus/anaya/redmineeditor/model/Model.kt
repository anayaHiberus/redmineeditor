package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.RedmineManager
import com.hiberus.anaya.redmineapi.runEachCatching
import com.hiberus.anaya.redmineeditor.controller.*
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/* ------------------------- settings ------------------------- */

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
     * the manager for online operations
     */
    protected lateinit var manager: RedmineManager

    /* ------------------------- compound data ------------------------- */

    /**
     * the current date: [month]+[day] (null if there is no current day)
     */
    val date
        get() = day?.let { month.atDay(it) }

    /**
     * true iff the current month was loaded (and data is valid)
     */
    val monthLoaded get() = month in manager.monthsLoaded

    /**
     * Calculates the hours spent in a date
     * TODO: return NONE (-1) if not loaded
     *
     * @param date date to check
     * @return hours spent that date
     */
    fun getSpent(date: LocalDate) =
        getEntriesForDate(date).sumOf { it.spent }

    /**
     * Calculates the hours spent in a month
     *
     * @param month month to check
     * @return hours spent that month
     */
    fun getSpent(month: YearMonth) =
        getEntriesForMonth(month).sumOf { it.spent }

    /**
     * the entries that should be displayed on the current day (empty if no current day)
     */
    val dayEntries
        get() = date?.let { getEntriesForDate(it) } ?: emptyList()

    /**
     * all distinct available issues (readonly)
     */
    val allIssues
        get() = manager.issues.toSet()

    /**
     * iff there is at least something that was modified (and should be uploaded)
     */
    val hasChanges get() = manager.hasChanges

    /* ------------------------- private getters ------------------------- */


    /**
     * return entries of a specific date
     * TODO replace with a map with date as key
     */
    protected fun getEntriesForDate(date: LocalDate) =
        manager.entries.filter { it.wasSpentOn(date) }

    /**
     * return entries of a specific month
     * TODO replace with a map with month as key
     */
    protected fun getEntriesForMonth(month: YearMonth) =
        manager.entries.filter { it.wasSpentOn(month) }


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

        override var month: YearMonth = YearMonth.now()
            set(value) {
                field = value
                changes += ChangeEvents.Month
            }

        override var day: Int? = LocalDate.now().dayOfMonth
            @Throws(MyException::class)
            set(value) {
                field = value?.takeIf { month.isValidDay(it) }
                prepareDay() // prepare day
                changes += ChangeEvents.Day // notify
            }

        /**
         * Loads the current month (if it is already loaded this does nothing)
         * Long operation
         *
         * @throws MyException on error
         */
        @Throws(MyException::class)
        fun loadDate() {
            // skip if already loaded or invalid settings
            if (month in manager.monthsLoaded || !SettingsLoaded) return

            try {
                // download
                manager.downloadEntriesFromMonth(month, prevDays).let { (newEntries, newIssues) ->
                    // and notify
                    if (newEntries) changes += ChangeEvents.Entries
                    if (newIssues) changes += ChangeEvents.Issues
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
            manager.uploadAll().convert {
                // on upload error
                MyException("Updating error", "An error occurred while updating data")
            }?.let { throw it }
        }

        /**
         * Creates a new time entry for current day (does nothing if there is no current day)
         *
         * @param issue for this issue
         */
        fun createTimeEntry(issue: Issue) =
            date?.let {
                manager.createTimeEntry(issue, it)
                changes += ChangeEvents.Entries
            } != null

        /**
         * Creates multiple new time entries for current date (does nothing if there is no current day)
         *
         * @param ids each one with an id from this
         */
        @Throws(MyException::class)
        fun createTimeEntries(ids: List<Int>) {
            date ?: return // skip now if there is no date

            try {
                // download missing issues, if any
                if (manager.downloadIssues(ids)) changes += ChangeEvents.Issues

                // create entries
                val missingIds = ids.filter { id ->
                    // create new entries for existing ids, and keep missing ids
                    manager.issues.firstOrNull { it.id == id }?.also { createTimeEntry(it) } == null
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
            date?.also { date ->

                // still not initialized
                if (!this::manager.isInitialized) return

                // skip if not loaded yet
                if (date.yearMonth !in manager.monthsLoaded)
                    return

                // add copy of past issues from previous days
                // for all entries in previous days (sorted by date)
                (0L..prevDays).flatMap { getEntriesForDate(date.minusDays(it)) }
                    // keep one for each issue
                    .distinctBy { it.issue }
                    // then remove those from today
                    .filterNot { it.wasSpentOn(date) }
                    // and create entry
                    .map {
                        manager.createTimeEntry(it.issue, date)
                            .apply { comment = it.comment }
                    }

                // add missing assigned issues for today
                val currentIssues = getEntriesForDate(date).map { it.issue }.distinct() // temp variable
                // get issues assigned to us
                manager.getAssignedIssues()
                    // not in today
                    .filterNot { it in currentIssues }
                    // and create empty entries
                    .map { manager.createTimeEntry(it, date) }

                // download all issues of today if configured
                if (SETTING.AUTO_LOAD_TOTAL_HOURS.value.toBoolean()) {
                    // load all issues of today
                    getEntriesForDate(date).map { it.issue }.distinct()
                        // and fill them
                        .runEachCatching { it.downloadSpent() }
                        .convert {
                            // background error
                            MyException("Issue exception", "Can't load issues data")
                        }?.let { throw it }
                }
            }
        }

        /**
         * clears and initializes the data
         */
        fun reload() {
            manager = RedmineManager(SETTING.URL.value, SETTING.KEY.value, SETTING.READ_ONLY.value.toBoolean())
            changes += setOf(ChangeEvents.Issues, ChangeEvents.Entries, ChangeEvents.Hours, ChangeEvents.Day, ChangeEvents.Month) // hack, to reload all
        }
    }

}

/**
 * YearMoth of a full date
 */
private val LocalDate.yearMonth get() = YearMonth.from(this)