package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException

/**
 * A redmine issue
 */
class Issue(
    rawIssue: JSONObject,
    private val manager: RedmineManager // an issue is associated to a manager
) {

    /* ------------------------- data ------------------------- */

    /**
     * The identifier
     */
    @JvmField
    val id: Int

    /**
     * The name of its project
     */
    @JvmField
    val project: String

    private val subject: String // The subject (title)

    /**
     * The description, may probably be empty
     */
    @JvmField
    val description: String

    /**
     * Estimated hours
     */
    var estimated: Double // estimated hours
        private set

    /**
     * Ratio of realization
     */
    var realization: Int // realization percentage
        private set

    /**
     * Total hours spent on this issue.
     * Can be not initialized
     */
    var spent: Double // total hours, uninitialized
        private set

    private val original: JSONObject? // the original raw object, for diff purposes


    /* ------------------------- constructors ------------------------- */

    init {
        // parse issue from raw json data
        original = rawIssue
        id = rawIssue.getInt("id")
        project = rawIssue.getJSONObject("project").optString("name", "")
        subject = rawIssue.optString("subject", "")
        description = rawIssue.optString("description")
        estimated = rawIssue.optDouble("estimated_hours", RedmineManager.NONE.toDouble())
        realization = rawIssue.optInt("done_ratio", 0)
        spent = rawIssue.optDouble("spent_hours", RedmineManager.UNINITIALIZED.toDouble())
    }

    /* ------------------------- properties ------------------------- */

    /**
     * @return the url to see this issue details
     */
    val url
        get() = "${manager.domain}issues/$id"

    /**
     * @return a short string describing this issue
     * @see .toString
     */
    fun toShortString() = "#$id: $subject"

    /**
     * @return a fairly long string describing this issue
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
        assert(spent >= 0)
        spent += amount
        assert(spent >= 0)
    }

    /**
     * Changes the estimated hours of this issue
     *
     * @param amount number of hours (negative to subtract)
     */
    fun addEstimated(amount: Double) {
        estimated = when {
            // still uninitialized, cancel
            estimated == RedmineManager.UNINITIALIZED.toDouble() -> return
            // no hours and want to subtract, disable
            estimated == 0.0 && amount < 0 -> RedmineManager.NONE.toDouble()
            // disabled and want to change, set to 0 if it wants to add, keep if not
            estimated == RedmineManager.NONE.toDouble() -> if (amount > 0) 0.0 else return
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
        if (spent == RedmineManager.UNINITIALIZED.toDouble()) {
            spent = get("${manager.domain}issues/$id.json?key=${manager.key}")
                .getJSONObject("issue").optDouble("spent_hours", RedmineManager.NONE.toDouble())
        }
    }

    /* ------------------------- uploading ------------------------- */

    /**
     * a list of changes made to this issue, as an object (empty means no changes)
     */
    private val changes
        get() = JSONObject().apply {
            // TODO: use a JSON as container so this can be a simple foreach diff
            if (original?.optDouble("estimated_hours", RedmineManager.NONE.toDouble()) != estimated) {
                // changed hours
                put("estimated_hours", estimated.takeIf { it != RedmineManager.NONE.toDouble() } ?: "")
            }
            if (original?.optInt("done_ratio", 0) != realization) {
                // changed ratio
                put("done_ratio", realization)
            }
        }

    /**
     * @return true if this entry requires upload, false otherwise
     */
    fun requiresUpload(): Boolean = !changes.isEmpty // no changes, no upload

    /**
     * Uploads an entry to redmine (unless not needed)
     *
     * @throws IOException on upload error
     */
    @Throws(IOException::class)
    fun uploadTimeEntry() =
        changes.let {
            if (it.isEmpty) return // ignore unmodified

            // update
            println("Updating issue $id with data: $changes")
            if (RedmineManager.OFFLINE) return
            if (put("${manager.domain}issues/$id.json?key=${manager.key}", JSONObject().put("issue", changes)) != 200) {
                throw IOException("Error when updating issue $id with data: $changes")
            }
        }

}