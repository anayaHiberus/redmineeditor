package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException

/**
 * A redmine issue
 */
class Issue(
    rawIssue: JSONObject,
    private val manager: RedmineManager, // an issue is associated to a manager
) {

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
     */
    var estimated: Double
        private set

    /**
     * Ratio of realization (percentage)
     */
    var realization: Int
        private set

    /**
     * Total hours spent on this issue.
     * Can be uninitialized
     */
    var spent: Double
        private set

    /**
     * the original raw object, if any, for diff purposes
     */
    private val original: JSONObject?

    /* ------------------------- constructors ------------------------- */

    init {
        original = rawIssue
        // parse issue from raw json data
        id = rawIssue.getInt("id")
        project = rawIssue.getJSONObject("project").optString("name", "")
        subject = rawIssue.optString("subject", "")
        description = rawIssue.optString("description")
        estimated = rawIssue.optDouble("estimated_hours", dNONE)
        realization = rawIssue.optInt("done_ratio", 0)
        spent = rawIssue.optDouble("spent_hours", dUNINITIALIZED)
    }

    /* ------------------------- properties ------------------------- */

    /**
     * the url to see this issue details
     */
    val url get() = "${manager.domain}issues/$id"

    /**
     * @return a short string describing this issue
     * @see .toString
     */
    fun toShortString() = "#$id: $subject"

    /**
     * @return a multiline string describing this issue
     * @see .toShortString
     */
    override fun toString() = "$project\n${toShortString()}"

    /* ------------------------- modifiers ------------------------- */

    /**
     * Changes the total hours spent on this issue
     *
     * @param amount number of hours (negative to subtract)
     */
    fun addSpent(amount: Double) {
        assert(spent.isSet) // assert initialized
        spent += amount
        assert(spent >= 0) // assert non-negative
    }

    /**
     * Changes the estimated hours of this issue
     *
     * @param amount number of hours (negative to subtract)
     */
    fun addEstimated(amount: Double) {
        estimated = when {
            // still uninitialized, cancel
            estimated == dUNINITIALIZED -> return
            // no hours and want to subtract, disable
            estimated == 0.0 && amount < 0 -> dNONE
            // disabled and want to change, set to 0 if it wants to add, keep if not
            estimated == dNONE -> if (amount > 0) 0.0 else return
            // else change, but don't subtract what can't be subtracted
            else -> (estimated + amount).coerceAtLeast(0.0)
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
     * Sets the realization to the spent/estimated percentage
     */
    fun syncRealization() {
        realization = (spent / estimated * 100).toInt().coerceIn(0, 100)
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
        if (spent != dUNINITIALIZED) return // skip initialized

        // load
        spent = manager.buildUrl("issues/$id").getJSON()
            .getJSONObject("issue")
            .optDouble("spent_hours", dNONE)
    }


    /* ------------------------- uploading ------------------------- */

    /**
     * a list of changes made to this issue, as an object (empty means no changes)
     */
    private val changes
        get() = JSONObject().apply {
            // TODO: use a JSON as container so this can be a simple foreach diff
            if (estimated != original?.optDouble("estimated_hours", dNONE)) {
                // changed hours
                put("estimated_hours", estimated.takeIf { it != dNONE } ?: "")
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
        if (OFFLINE) return
        JSONObject().put("issue", changes)
            .putTo(manager.buildUrl("issues/$id"))
            .ifNot(200) { throw IOException("Error when updating issue $id with data: $changes") }

    }

}