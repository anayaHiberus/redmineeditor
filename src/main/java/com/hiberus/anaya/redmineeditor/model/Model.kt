package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.RedmineManager
import com.hiberus.anaya.redmineeditor.controller.*
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
     * calculates the hours spent in a [date], null if the date is not loaded
     */
    fun getSpent(date: LocalDate) =
        manager.getEntriesForDate(date)?.sumOf { it.spent }

    /**
     * calculates the hours spent in a [month], null if the month is not loaded
     */
    fun getSpent(month: YearMonth) =
        manager.getEntriesForMonth(month)?.sumOf { it.spent }

    /**
     * the entries that should be displayed on the current day (null if no current day or not loaded)
     */
    val dayEntries
        get() = date?.let { manager.getEntriesForDate(it) }

    /**
     * all distinct loaded issues (readonly)
     */
    val loadedIssues
        get() = manager.loadedIssues.toSet()

    /**
     * iff there is at least something that was modified (and should be uploaded)
     */
    val hasChanges get() = manager.hasChanges

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
            // skip if invalid settings
            if (!SettingsLoaded) return

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
            date?.let { date ->
                manager.createTimeEntry(issue, date).also {
                    // autoload if required, ignore errors
                    if (autoLoadTotalHours) {
                        runCatching { it.issue.downloadSpent() }.onFailure {
                            // warning
                            System.err.println("Error when loading spent, ignoring: $it")
                        }
                    }
                }
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
                    manager.loadedIssues.firstOrNull { it.id == id }?.also { createTimeEntry(it) } == null
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

                // add copy of past issues from previous days
                // for all entries in previous days (sorted by date)
                (0L..prevDays).flatMap { manager.getEntriesForDate(date.minusDays(it)) ?: return } // skip if not loaded yet
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
                val currentIssues = (manager.getEntriesForDate(date) ?: return).map { it.issue }.distinct() // temp variable
                // get issues assigned to us
                manager.getAssignedIssues()
                    // not in today
                    .filterNot { it in currentIssues }
                    // and create empty entries
                    .map { manager.createTimeEntry(it, date) }

                // download all issues of today if configured
                if (autoLoadTotalHours) {
                    // load all issues of today
                    (manager.getEntriesForDate(date) ?: return).map { it.issue }.distinct().forEach {
                        runCatching { it.downloadSpent() }.onFailure {
                            // warning
                            System.err.println("Error when loading spent, ignoring: $it")
                        }
                    }
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