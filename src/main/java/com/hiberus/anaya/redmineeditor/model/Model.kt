package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineapi.RedmineManager
import com.hiberus.anaya.redmineapi.TimeEntry
import com.hiberus.anaya.redmineeditor.controller.*
import org.json.JSONException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/**
 * Number of days for 'past' computations
 */
const val PREV_DAYS = 7

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
    val manager = RedmineManager(SETTING.URL.value, SETTING.KEY.value)

    /**
     * time entries
     */
    val entries = mutableListOf<TimeEntry>()

    /**
     * Loaded issues
     */
    val issues = mutableListOf<Issue>()

    /**
     * months that are already loaded and don't need to be again
     */
    val monthsLoaded = mutableSetOf<YearMonth>()

    /* ------------------------- compound data ------------------------- */

    /**
     * the current date: [month]+[day] (null if there is no current day)
     */
    val date
        get() = day?.let { month.atDay(it) }

    /**
     * Calculates the hours spent in a date
     * TODO: return NONE (-1) if not loaded
     *
     * @param date date to check
     * @return hours spent that date
     */
    fun getSpent(date: LocalDate) =
        _getEntriesForDate(date).sumOf { it.spent }

    /**
     * Calculates the hours spent in a month
     *
     * @param month month to check
     * @return hours spent that month
     */
    fun getSpent(month: YearMonth) =
        _getEntriesForMonth(month).sumOf { it.spent }

    /**
     * the entries that should be displayed on the current day (empty if no current day)
     */
    val dayEntries
        get() =
            date?.let { _getEntriesForDate(it) } ?: emptyList()

    /**
     * all distinct available issues
     */
    val allIssues
        get() = issues.toList()

    /**
     * iff there is at least something that was modified (and should be uploaded)
     */
    val hasChanges
        get() = entries.any { it.requiresUpload } || issues.any { it.requiresUpload }

    /* ------------------------- private getters ------------------------- */

    /**
     * gets the loaded issue with the given id, null if not present
     */
    internal fun _getIssueFromId(id: Int) =
        issues.firstOrNull { it.id == id }

    /**
     * return entries of a specific date
     * TODO replace with a map with date as key
     */
    internal fun _getEntriesForDate(date: LocalDate) =
        entries.filter { it.wasSpentOn(date) }

    /**
     * return entries of a specific month
     * TODO replace with a map with month as key
     */
    private fun _getEntriesForMonth(month: YearMonth) =
        entries.filter { it.wasSpentOn(month) }

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

        override var isLoading: Boolean = true
            set(value) {
                field = value
                changes += ChangeEvents.Loading
            }

        override var month: YearMonth = YearMonth.now()
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
         * Loads the current month (if it is already loaded this does nothing)
         * Long operation
         *
         * @throws MyException on error
         */
        @Throws(MyException::class)
        fun loadMonth() {
            // skip if already loaded
            if (month in monthsLoaded) return

            try {
                // load from the internet all entries in month
                entries += manager.getTimeEntries(
                    month.atDay(1)
                        .ifCheck(month.minusMonths(1) !in monthsLoaded) {
                            // load previous days if previous month was not loaded
                            minusDays(PREV_DAYS.toLong())
                        },
                    month.atEndOfMonth()
                        .ifCheck(month.plusMonths(1) in monthsLoaded) {
                            // don't load last days if next month was loaded
                            minusDays(PREV_DAYS.toLong())
                        },
                    issues
                )
            } catch (e: IOException) {
                throw MyException("Network error", "Can't load content from Redmine. Try again later.", e)
            } catch (e: JSONException) {
                throw MyException("Parsing error", "Unknown Redmine response. Try again later.", e)
            }

            // prepare
            prepareDay()

            // mark
            monthsLoaded += month
            changes += ChangeEvents.Entries
            changes += ChangeEvents.Issues // TODO: don't notify if no new entries are loaded
        }

        /**
         * Discards all entries and issues
         */
        fun clearAll() {
            monthsLoaded.clear()
            entries.clear()
            changes += ChangeEvents.Entries
            changes += ChangeEvents.Month // technically month doesn't change, but its data does, this forces a reload in calendar

            issues.clear()
            changes += ChangeEvents.Issues
        }

        /**
         * Uploads all modified data
         * Long operation
         *
         * @throws MyException on error
         */
        @Throws(MyException::class)
        fun uploadAll() {
            // TODO: move all this logic to manager
            (entries.runEachCatching { it.upload() }
                    + issues.runEachCatching { it.upload() })
                .convert {
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
                entries += manager.newTimeEntry(issue, it)
                changes += ChangeEvents.Entries
            } != null

        /**
         * Creates multiple new time entries for current date (does nothing if there is no current day)
         *
         * @param ids each one with an id from this
         */
        @Throws(MyException::class)
        fun createTimeEntries(ids: Sequence<Int>) {
            date ?: return // skip now if there is no date

            val pendingIds = ids.filter { id ->
                // if issue exists, create new entry directly
                // if not, keep for later loading
                _getIssueFromId(id)?.also { createTimeEntry(it) } == null
            }.toMutableList()

            try {
                manager.getIssues(pendingIds)
                    // create and add issues
                    .onEach {
                        createTimeEntry(it)
                        issues += it
                        changes += ChangeEvents.Issues
                    }
                    // remove from loaded
                    .map { it.id }.let { pendingIds -= it }

                when {
                    // missing single issue
                    pendingIds.size == 1 ->
                        throw MyException("Unknown issue", "The issue #${pendingIds[0]} was not found or couldn't be loaded", warning = true)
                    // missing multiple issues
                    pendingIds.size >= 2 ->
                        throw MyException("Unknown issues", "The issues ${pendingIds.joinToString(", ") { "#$it" }} were not found or couldn't be loaded", warning = true)
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
        private fun prepareDay() = date?.let { date ->

            // for all entries in previous days (sorted by date)
            (0L..PREV_DAYS).flatMap { _getEntriesForDate(date.minusDays(it)) }
                // keep one for each issue
                .distinctBy { it.issue }
                // and create copies for today if not already
                .onEach {
                    if (!it.wasSpentOn(date))
                        entries += manager.newTimeEntry(it.issue, date)
                            .apply { comment = it.comment }
                }
                // keep issues
                .map { it.issue }
                // fill them
                .runEachCatching { it.downloadSpent() }
                .convert {
                    // background error
                    MyException("Issue exception", "Can't load issues data")
                }?.let { throw it }
        }
    }

}

/**
 * If check is true, apply then
 */
private fun <T> T.ifCheck(check: Boolean, then: T.() -> T) = if (check) then() else this


