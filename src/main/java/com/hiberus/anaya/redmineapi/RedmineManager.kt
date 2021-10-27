package com.hiberus.anaya.redmineapi

import com.hiberus.anaya.redmineeditor.controller.MyException
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/**
 * Redmine API.
 * The 'official' one is not used because it doesn't allow searching with multiple filters
 */
class RedmineManager {

    /* ------------------------- constructor ------------------------- */

    /**
     * @param domain the redmine domain
     * @param key the redmine api key
     * @param read_only if true, put/post petitions will be skipped (but still logged)
     */
    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(domain: String, key: String, read_only: Boolean = false) {
        this.connector = Connector(domain, key, read_only)
    }

    /* ------------------------- private data ------------------------- */

    /**
     * The connector
     */
    private var connector: Connector

    /**
     * if assigned issues are already loaded
     */
    private var assignedLoaded = false

    /* ------------------------- public data ------------------------- */

    /**
     * time entries
     */
    val entries = mutableListOf<TimeEntry>()

    /**
     * Loaded issues
     */
    val issues = mutableSetOf<Issue>()

    /**
     * months that are already loaded and don't need to be again
     * TODO: remove this (make it private) and replace with nulls when requesting data that doesn't exists
     */
    val monthsLoaded = mutableSetOf<YearMonth>()

    /**
     * iff there is at least something that was modified (and should be uploaded)
     */
    val hasChanges
        get() = entries.any { it.requiresUpload } || issues.any { it.requiresUpload }

    /* ------------------------- getters ------------------------- */

    /**
     * return assigned issues
     */
    @Throws(IOException::class)
    fun getAssignedIssues(): List<Issue> {
        // download if not yet
        if (!assignedLoaded) {
            issues += connector.downloadAssignedIssues()
            assignedLoaded = true
        }

        // filter
        return issues.filter { it.assigned_to == (connector.userId ?: return emptyList()) }
    }

    /* ------------------------- loaders ------------------------- */

    /**
     * downloads entries for a given [month] with +-[prevDays] (unless already loaded)
     * returns a pair of booleans: newentries, newissues
     */
    @Throws(IOException::class)
    fun downloadEntriesFromMonth(month: YearMonth, prevDays: Long): Pair<Boolean, Boolean> {
        if (month in monthsLoaded) return false to false // already loaded

        // load from the internet all entries in month
        connector.downloadTimeEntries(
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
            issues
        ).let { (newEntries, newIssues) ->
            // and save them
            entries += newEntries
            issues += newIssues
            monthsLoaded += month
            return newEntries.isNotEmpty() to newIssues.isNotEmpty()
        }
    }

    /**
     * Uploads all needed data
     */
    @Throws(IOException::class)
    fun uploadAll() =
        (entries.runEachCatching { it.upload() }
                + issues.runEachCatching { it.upload() })

    /**
     * Creates a new Time Entry
     *
     * @param issue    issue for the entry
     * @param spent_on day this entry is spent on
     * @return the created entry
     */
    fun createTimeEntry(issue: Issue, spent_on: LocalDate) =
        TimeEntry(issue, spent_on, connector).also { entries += it }

    /**
     * Downloads issues if required from their [ids]
     * returns true if there was at least one downloaded
     */
    @Throws(MyException::class)
    fun downloadIssues(ids: List<Int>): Boolean {
        // load missing
        val loadedIds = issues.map { it.id }
        connector.downloadIssues(ids.filter { it !in loadedIds }).let {
            // save and return
            issues += it
            return it.isNotEmpty()
        }
    }

}

/* ------------------------- utils ------------------------- */

/**
 * If check is true, apply and return then, else keep this
 */
private fun <T> T.ifCheck(check: Boolean, then: T.() -> T) = if (check) then() else this


/**
 * Runs [function] on each element, returns a list of all the exceptions thrown
 */
private fun <T> Iterable<T>.runEachCatching(function: (T) -> Unit) =
    mapNotNull {
        runCatching {
            function(it)
        }.exceptionOrNull()
    }