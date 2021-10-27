package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth

/**
 * 'You spent X hours into an issue with a message' object
 */
class TimeEntry {

    /* ------------------------- manager ------------------------- */

    private val connector: Connector // an entry is associated to a connector

    /* ------------------------- data ------------------------- */

    /**
     * the entry id in the database
     */
    val id: Int

    /**
     * the issue id
     */
    val issue: Issue

    /**
     * date it was spent
     */
    private val spent_on: LocalDate

    /**
     * the spent hours of this entry
     */
    var spent: Double
        private set

    /**
     * the comment of this entry
     */
    var comment: String

    /**
     * the original raw object, for diff purposes. Null if there is no original
     */
    private var original: JSONObject? = null


    /* ------------------------- constructors ------------------------- */

    /**
     * Creates a new entry from a json [rawEntry]
     * Issues must contain the issue!
     */
    internal constructor(rawEntry: JSONObject, issues: Iterable<Issue>, manager: Connector) {
        this.connector = manager
        original = rawEntry
        // creates a new entry from a json raw data
        id = rawEntry.getInt("id")
        issue = issues.first { issue: Issue -> issue.id == getIssueId(rawEntry) }
        spent_on = LocalDate.parse(rawEntry.getString("spent_on"))
        spent = rawEntry.getDouble("hours")
        comment = rawEntry.optString("comments")
    }

    /**
     * Creates a new time entry for an existing [issue] and [spent_on] date
     */
    internal constructor(issue: Issue, spent_on: LocalDate, manager: Connector) {
        this.connector = manager
        id = iNONE
        this.issue = issue
        this.spent_on = spent_on
        spent = 0.0
        comment = ""
    }

    /* ------------------------- checks ------------------------- */

    /**
     * Checks if this entry was spent on a specific date
     *
     * @param date check spent with this date
     * @return true iff this entry was spent on that date
     */
    fun wasSpentOn(date: LocalDate) = spent_on == date

    /**
     * Checks if this entry was spent on a specific month
     *
     * @param month check spent with this month
     * @return true iff this entry was spent on that month
     */
    fun wasSpentOn(month: YearMonth) = YearMonth.from(spent_on) == month

    /* ------------------------- modifiers ------------------------- */

    /**
     * @param amount new hours to add to this entry (negative to subtract)
     */
    fun addSpent(amount: Double) =
        // don't subtract what can't be subtracted
        ((spent + amount).coerceAtLeast(0.0) - spent).let {
            spent += it
            issue.addSpent(it)
        }

    /* ------------------------- uploading ------------------------- */

    /**
     * a list of changes made to this entry, as an object (empty means no changes)
     */
    private val changes
        get() = JSONObject().apply {
            if (spent != original?.getDouble("hours")) {
                // changed hours
                put("hours", spent)
            }
            if (comment != original?.optString("comments")) {
                // changed comment
                put("comments", comment)
            }
            if (id == iNONE) {
                // without original, this data is considered new
                put("issue_id", issue.id)
                put("spent_on", spent_on)
            }
        }

    /**
     * iff this entry requires upload, false otherwise
     */
    val requiresUpload
        get() = !(changes.isEmpty // no changes, no upload
                || (id == iNONE && spent <= 0)) // no useful changes, no upload

    /**
     * Uploads an entry to redmine (unless not needed)
     *
     * @throws IOException on upload error
     */
    @Throws(IOException::class)
    fun upload() {
        changes.takeUnless { it.isEmpty }?.also { // if there are changes
            when {
                id == iNONE && spent > 0 -> {
                    // new entry with hours, create
                    println("Creating entry with data: $it")
                    if (connector.read_only) return
                    JSONObject().put("time_entry", it)
                        .postTo(connector.buildUrl("time_entries"))
                        .ifNot(201) {
                            throw IOException("Error when creating entry with data: $it")
                        }
                }

                // new entry without hours, ignore
                id == iNONE && spent <= 0 -> Unit

                id >= 0 && spent > 0 -> {
                    // existing entry with hours, update
                    println("Updating entry $id with data: $it")
                    if (connector.read_only) return
                    JSONObject().put("time_entry", it)
                        .putTo(connector.buildUrl("time_entries/$id"))
                        .ifNot(200) {
                            throw IOException("Error when updating entry $id with data: $it")
                        }
                }

                id >= 0 && spent <= 0 -> {
                    // existing entry without hours, delete
                    println("Deleting entry $id")
                    if (connector.read_only) return
                    connector.buildUrl("time_entries/$id")
                        .delete()
                        .ifNot(200) {
                            throw IOException("Error when deleting entry $id")
                        }
                }

                // should never happen, but if it does, do nothing
                else -> Unit
            }
        }
    }
}

/* ------------------------- util ------------------------- */

internal fun getIssueId(rawEntry: JSONObject) = rawEntry.getJSONObject("issue").getInt("id") // returns the id from a rawEntry