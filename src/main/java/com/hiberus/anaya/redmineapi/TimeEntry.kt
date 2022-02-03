package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/**
 * 'You spent X hours into an issue with a message' object
 */
class TimeEntry {

    /* ------------------------- manager ------------------------- */

    private val remote: Remote // for remote interactions

    /* ------------------------- data ------------------------- */

    /**
     * the entry id in the database, null if this is a new entry without an associated id
     */
    val id: Int?

    /**
     * the issue id
     */
    var issue: Issue

    /**
     * date it was spent
     */
    var spent_on: LocalDate

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
    internal constructor(rawEntry: JSONObject, issues: Iterable<Issue>, remote: Remote) {
        this.remote = remote
        original = rawEntry
        // creates a new entry from a json raw data
        id = rawEntry.getInt("id")
        issue = issues.first { issue: Issue -> issue.id == rawEntry.getIssueId() }
        spent_on = rawEntry.getSpentOnDate()
        spent = rawEntry.getDouble("hours")
        comment = rawEntry.optString("comments")
    }

    /**
     * Creates a new Time Entry for [issue] on [spent_on] with already [spent] hours and [comment]
     */
    internal constructor(issue: Issue, spent_on: LocalDate, spent: Double, comment: String, remote: Remote) {
        this.remote = remote
        id = null
        this.issue = issue
        this.spent_on = spent_on
        this.spent = spent
        this.comment = comment
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

    /**
     * changes the spent hours to any [amount]
     */
    fun changeSpent(amount: Double) = addSpent(amount - spent)

    /* ------------------------- uploading ------------------------- */

    /**
     * a list of changes made to this entry, as an object (empty means no changes)
     */
    internal val changes
        get() = JSONObject().apply {
            if (spent != original?.getDouble("hours")) {
                // changed hours
                put("hours", spent)
            }
            if (comment != original?.optString("comments")) {
                // changed comment
                put("comments", comment)
            }
            if (spent_on != original?.getSpentOnDate()
                || id == null
            ) {
                // change date, or new entry
                put("spent_on", spent_on)
            }
            if (issue.id != original?.getIssueId()
                || id == null
            ) {
                // change issue, or new entry
                put("issue_id", issue.id)
            }
        }

    /**
     * iff this entry requires upload, false otherwise
     */
    val requiresUpload
        get() = !(changes.isEmpty // no changes, no upload
                || (id == null && spent <= 0)) // no useful changes, no upload

}

/* ------------------------- util ------------------------- */

internal fun JSONObject.getIssueId() = getJSONObject("issue").getInt("id") // returns the id from a rawEntry
internal fun JSONObject.getSpentOnDate() = LocalDate.parse(getString("spent_on")) // returns the spent_on date from a rawEntry
