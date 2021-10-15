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

    private val manager: RedmineManager // an entry is associated to a manager

    /* ------------------------- data ------------------------- */

    val id: Int // the entry id in the database

    /**
     * the issue id
     */
    @JvmField
    val issue: Issue

    private val spent_on: LocalDate // date it was spent

    /**
     * the spent hours of this entry
     */
    var spent = 0.0 // hours spent
        private set

    /**
     * the comment of this entry
     */
    var comment = "" // comment


    private var original: JSONObject? = null // the original raw object, for diff purposes. Null if there is no original


    /* ------------------------- constructors ------------------------- */

    internal constructor(rawEntry: JSONObject, issues: List<Issue>, manager: RedmineManager) {
        // creates a new entry from a json raw data
        this.manager = manager
        original = rawEntry
        id = rawEntry.getInt("id")
        issue = issues.first { issue: Issue -> issue.id == getIssueId(rawEntry) }
        spent_on = LocalDate.parse(rawEntry.getString("spent_on"))
        spent = rawEntry.getDouble("hours")
        comment = rawEntry.optString("comments")
    }

    internal constructor(issue: Issue, spent_on: LocalDate, manager: RedmineManager) {
        // Creates a new time entry for an existing issue and date
        this.manager = manager
        id = NONE
        this.issue = issue
        this.spent_on = spent_on
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
    fun addSpent(amount: Double) = ((spent + amount).coerceAtLeast(0.0) - spent).let {   // don't subtract what can't be substracted
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
            if (id == NONE) {
                // without original, this data is considered new
                put("issue_id", issue.id)
                put("spent_on", spent_on)
            }
        }

    /**
     * @return true if this entry requires upload, false otherwise
     */
    fun requiresUpload() = !(
            changes.isEmpty // no changes, no upload
                    ||
                    (id == NONE && spent <= 0) // no useful changes, no upload
            )

    /**
     * Uploads an entry to redmine (unless not needed)
     *
     * @throws IOException on upload error
     */
    @Throws(IOException::class)
    fun uploadTimeEntry() =
        changes.let { // get changes
            if (it.isEmpty) return // ignore unmodified

            if (id == NONE) {
                if (spent > 0) {
                    // new entry with hours, create
                    println("Creating entry with data: $it")
                    if (OFFLINE) return
                    if (post("${manager.domain}time_entries.json?key=${manager.key}", JSONObject().put("time_entry", it)) != 201) {
                        throw IOException("Error when creating entry with data: $it")
                    }
                }
                //new entry without hours, ignore
            } else {
                if (spent > 0) {
                    // existing entry with hours, update
                    println("Updating entry $id with data: $it")
                    if (OFFLINE) return
                    if (put("${manager.domain}time_entries/$id.json?key=${manager.key}", JSONObject().put("time_entry", it)) != 200) {
                        throw IOException("Error when updating entry $id with data: $it")
                    }
                } else {
                    // existing entry without hours, delete
                    println("Deleting entry $id")
                    if (OFFLINE) return
                    if (delete("${manager.domain}time_entries/$id.json?key=${manager.key}") != 200) {
                        throw IOException("Error when deleting entry $id")
                    }
                }
            }
        }
}

/* ------------------------- util ------------------------- */

fun getIssueId(rawEntry: JSONObject) = rawEntry.getJSONObject("issue").getInt("id") // returns the id from a rawEntry