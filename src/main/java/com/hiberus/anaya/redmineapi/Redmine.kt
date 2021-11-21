package com.hiberus.anaya.redmineapi

import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/* ------------------------- Global settings ------------------------- */

/**
 * if true, put/post petitions will be skipped (but still logged)
 */
var READ_ONLY = false

/* ------------------------- Main ------------------------- */

/**
 * Redmine API.
 * The 'official' one is not used because it doesn't allow searching with multiple filters
 */
class Redmine {

    /* ------------------------- constructor ------------------------- */

    /**
     * @param domain the redmine domain
     * @param key the redmine api key
     */
    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(domain: String, key: String, prevDays: Long) {
        this.remote = Remote(domain, key)
        this.prevDays = prevDays
    }

    /* ------------------------- private data ------------------------- */

    /**
     * The remote
     */
    private var remote: Remote

    /**
     * if assigned issues are already loaded
     */
    private var assignedLoaded = false

    /**
     * months that are already loaded and don't need to be again
     */
    private val monthsLoaded = mutableSetOf<YearMonth>()

    /**
     * Number of days for 'past' computations
     */
    private var prevDays: Long

    /* ------------------------- public data ------------------------- */

    /**
     * loaded time entries
     */
    private val loadedEntries = mutableListOf<TimeEntry>()

    /**
     * Loaded issues
     */
    val loadedIssues = mutableSetOf<Issue>()

    /**
     * iff there is at least something that was modified (and should be uploaded)
     */
    val hasChanges
        get() = loadedEntries.any { it.requiresUpload } || loadedIssues.any { it.requiresUpload }

    /* ------------------------- getters ------------------------- */

    /**
     * return assigned issues
     */
    @Throws(IOException::class)
    fun getAssignedIssues(): List<Issue> {
        // download if not yet
        if (!assignedLoaded) {
            val loadedIssuesIds = loadedIssues.map { it.id } // temp
            loadedIssues += remote.downloadAssignedIssues().filter { it.id !in loadedIssuesIds } // skip already loaded
            assignedLoaded = true
        }

        // filter
        return loadedIssues.filter { it.assigned_to == (remote.userId ?: return emptyList()) }
    }


    /**
     * return entries of a specific date
     */
    fun getEntriesForDate(date: LocalDate) =
        date.takeIf { it.yearMonth in monthsLoaded || it.plusDays(prevDays).yearMonth in monthsLoaded }?.run { loadedEntries.filter { it.wasSpentOn(this) } }

    /**
     * return entries of a specific month
     */
    fun getEntriesForMonth(month: YearMonth) =
        month.takeIf { it in monthsLoaded }?.run { loadedEntries.filter { it.wasSpentOn(this) } }

    /* ------------------------- loaders ------------------------- */

    /**
     * downloads entries for a given [month] with +-[prevDays] (unless already loaded)
     * returns a pair of booleans: newEntries, newIssues
     */
    @Throws(IOException::class)
    fun downloadEntriesFromMonth(month: YearMonth): Pair<Boolean, Boolean> {
        if (month in monthsLoaded) return false to false // already loaded

        // load from the internet all entries in month
        remote.downloadTimeEntries(
            month.atDay(1)
                .ifCheck(month.minusMonths(1) !in monthsLoaded) {
                    // load previous days if previous month was not loaded
                    minusDays(prevDays)
                },
            month.atEndOfMonth()
                .ifCheck(month.plusMonths(1) in monthsLoaded) {
                    // don't load last days if next month was loaded
                    minusDays(prevDays)
                },
            loadedIssues
        ).let { (newEntries, newIssues) ->
            // and save them
            loadedEntries += newEntries
            loadedIssues += newIssues
            monthsLoaded += month
            return newEntries.isNotEmpty() to newIssues.isNotEmpty()
        }
    }

    /**
     * Uploads all needed data
     */
    @Throws(IOException::class)
    fun uploadAll() =
        (loadedEntries.runEachCatching { remote.upload(it) }
                + loadedIssues.runEachCatching { remote.upload(it) })

    /**
     * Creates a new Time Entry for [issue] on [spent_on] with already [spent] hours and [comment], and returns it
     */
    fun createTimeEntry(issue: Issue, spent_on: LocalDate, spent: Double = 0.0, comment: String = "") =
        TimeEntry(issue, spent_on, spent, comment, remote).also { loadedEntries += it }
            .also { issue.addSpent(spent) } // update issue hours too

    /**
     * Downloads issues if required from their [ids]
     * returns true if there was at least one downloaded
     */
    @Throws(IOException::class)
    fun downloadIssues(ids: List<Int>): Boolean {
        // load missing
        val loadedIds = loadedIssues.map { it.id }
        remote.downloadIssues(ids.filter { it !in loadedIds }).let {
            // save and return
            loadedIssues += it
            return it.isNotEmpty()
        }
    }

    /**
     * Returns the username, can be used as a way to check if the settings are ok
     */
    @Throws(IOException::class)
    fun getUserName(appendLogin: Boolean = false) = remote.getUserName(appendLogin)

}

/* ------------------------- utils ------------------------- */

/**
 * If check is true, apply and return then, else keep this
 */
private inline fun <T> T.ifCheck(check: Boolean, then: T.() -> T) = if (check) then() else this


/**
 * Runs [function] on each element, returns a list of all the exceptions thrown
 */
private fun <T> Iterable<T>.runEachCatching(function: (T) -> Unit) =
    mapNotNull {
        runCatching {
            function(it)
        }.exceptionOrNull()
    }

/**
 * YearMoth of a full date
 */
private val LocalDate.yearMonth get() = YearMonth.from(this)