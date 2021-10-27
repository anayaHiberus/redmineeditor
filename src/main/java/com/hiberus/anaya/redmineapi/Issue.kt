package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException

/**
 * A redmine issue
 */
class Issue {

    /* ------------------------- manager ------------------------- */

    private val connector: Connector // an entry is associated to a connector

    /* ------------------------- data ------------------------- */

    /**
     * The identifier
     */
    val id: Int

    /**
     * The name of its project
     */
    val project: String

    /**
     * The subject (title)
     */
    private val subject: String

    /**
     * The description, may probably be empty
     */
    val description: String

    /**
     * Estimated hours
     * null if unset
     */
    var estimated: Double?
        private set

    /**
     * Ratio of realization (percentage)
     */
    var realization: Int
        private set

    /**
     * Total hours spent on this issue.
     * null if uninitialized
     */
    var spent: Double?
        private set

    /**
     * [spent]/[estimated]
     * null if invalid
     */
    val spent_realization: Int?
        get() {
            return ((spent?.takeIf { it >= 0 } ?: return null) / (estimated?.takeIf { it > 0 } ?: return null) * 100).toInt()
        }

    /**
     * Id of the assigned user
     */
    val assigned_to: Int?

    /**
     * the original raw object, if any, for diff purposes
     */
    private val original: JSONObject?

    /* ------------------------- constructors ------------------------- */

    // an issue is associated to a manager
    @Suppress("ConvertSecondaryConstructorToPrimary")
    internal constructor(rawIssue: JSONObject, manager: Connector) {
        this.connector = manager
        original = rawIssue
        id = rawIssue.getInt("id")
        project = rawIssue.getJSONObject("project").optString("name")
        subject = rawIssue.optString("subject", "")
        description = rawIssue.optString("description")
        estimated = rawIssue.noNaNDouble("estimated_hours")
        realization = rawIssue.optInt("done_ratio", 0)
        spent = rawIssue.noNaNDouble("spent_hours")
        assigned_to = rawIssue.optJSONObject("assigned_to")?.getInt("id")
    }

    /* ------------------------- properties ------------------------- */

    /**
     * the url to see this issue details
     */
    val url get() = "${connector.domain}issues/$id"

    /**
     * @return a short string describing this issue
     * @see .toString
     */
    fun toShortString() = "#$id: $subject${connector.userId?.takeIf { it == assigned_to }?.let { " [you]" } ?: ""}"

    /**
     * @return a multiline string describing this issue
     * @see .toShortString
     */
    override fun toString() = "$project\n#$id: $subject" + (connector.userId?.takeIf { it == assigned_to }?.let { "\nAssigned to you" } ?: "")

    /* ------------------------- modifiers ------------------------- */

    /**
     * Changes the total hours spent on this issue
     *
     * @param amount number of hours (negative to subtract)
     */
    fun addSpent(amount: Double) {
        assert(spent != null) // assert initialized
        spent?.let { spent = it + amount }
        assert(spent?.let { it >= 0 } ?: false) // assert non-negative
    }

    /**
     * Changes the estimated hours of this issue
     *
     * @param amount number of hours (negative to subtract)
     */
    fun addEstimated(amount: Double) {
        estimated = estimated?.let {
            // no hours and want to subtract, disable
            if (it == 0.0 && amount < 0) null
            // else change, but don't subtract what can't be subtracted
            else (it + amount).coerceAtLeast(0.0)
        } ?: run {
            // disabled and want to change, set to 0 if it wants to add, keep if not
            if (amount > 0) 0.0 else null
        }
    }

    /**
     * Changes the realization percentage of this issue
     *
     * @param amount percentage (negative to subtract)
     */
    fun addRealization(amount: Int) {
        realization = (realization + amount).coerceIn(0, 100) // don't subtract what can't be subtracted, and don't add what can be added
    }

    /**
     * Sets the realization to the spent/estimated percentage, if loaded
     */
    fun syncRealization() {
        realization = spent_realization?.coerceIn(0, 100) ?: return
    }

    /* ------------------------- downloading ------------------------- */

    /**
     * Loads spent hours (nothing if already initialized)
     * Long operation
     *
     * @throws IOException on network error
     */
    @Throws(IOException::class)
    fun downloadSpent() {
        spent?.let { return } // skip initialized

        // load
        spent = connector.buildUrl("issues/$id").getJSON()
            .getJSONObject("issue")
            .noNaNDouble("spent_hours")
    }


    /* ------------------------- uploading ------------------------- */

    /**
     * a list of changes made to this issue, as an object (empty means no changes)
     */
    private val changes
        get() = JSONObject().apply {
            // TODO: maybe use a JSON as container so this can be a simple foreach diff
            if (original?.run { noNaNDouble("estimated_hours") != estimated } != false) { // if original is null or estimated is different (true != false)
                // changed hours
                put("estimated_hours", estimated ?: "")
            }
            if (realization != original?.optInt("done_ratio", 0)) {
                // changed ratio
                put("done_ratio", realization)
            }
        }

    /**
     * iff this entry requires upload, false otherwise
     */
    val requiresUpload get() = !changes.isEmpty

    /**
     * Uploads an entry to redmine (unless not needed)
     *
     * @throws IOException on upload error
     */
    @Throws(IOException::class)
    fun upload() = changes.run {
        if (isEmpty) return // ignore unmodified

        // update
        println("Updating issue $id with data: $changes")
        if (connector.read_only) return
        JSONObject().put("issue", changes)
            .putTo(connector.buildUrl("issues/$id"))
            .ifNot(200) { throw IOException("Error when updating issue $id with data: $changes") }

    }

}

/* ------------------------- utils ------------------------- */

/**
 * Get an optional double associated with a key, or null if there is no such key or if its value is not a number (NaN). If the value is a string, an attempt will be made to evaluate it as a number.
 */
private fun JSONObject.noNaNDouble(key: String) = optDouble(key).takeIf { !it.isNaN() }