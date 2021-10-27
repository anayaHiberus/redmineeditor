package com.hiberus.anaya.redmineapi

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.time.LocalDate

/* ------------------------- defaults ------------------------- */
// TODO: remove this and replace with null

/**
 * Unset int property
 */
const val iNONE = -1

/**
 * Check if the value is set (not NONE)
 */
//val Int.isSet get() = this >= 0

/**
 * Unset double property
 */
const val dNONE = -1.0

/**
 * Not initialized double property
 */
const val dUNINITIALIZED = -2.0

/**
 * Check if the value is set (not NONE nor UNINITIALIZED)
 */
val Double.isSet get() = this >= 0.0

/* ------------------------- class ------------------------- */

/**
 * Manages connections with the api
 * @param domain the redmine domain
 * @param key the redmine api key
 * @param read_only if true, put/post petitions will be skipped (but still logged)
 */
internal class Connector(
    val domain: String,
    private val key: String,
    val read_only: Boolean,
) {

    /* ------------------------- properties ------------------------- */

    /**
     * Build a redmine json api url for a given path (including key)
     */
    fun buildUrl(path: String) = URL("$domain$path.json?key=$key")

    /**
     * Loaded id of the user (null if uninitialized)
     */
    var userId: Int? = null

    /* ------------------------- time entries ------------------------- */

    /**
     * Returns all the entries on a timeframe for the current user
     *
     * @param from         from this date (included)
     * @param to           to this date (included)
     * @param loadedIssues issues already loaded. New ones will be added here!!!
     * @return the list of entries from that data
     * @throws IOException if network failed
     */
    @Throws(IOException::class)
    fun downloadTimeEntries(from: LocalDate, to: LocalDate, loadedIssues: Set<Issue>): Pair<List<TimeEntry>, MutableList<Issue>> {
        val newIssues = mutableListOf<Issue>()
        val entries = (
                "${domain}time_entries.json?utf8=✓&"
                        + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=me"
                        + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=$from&v[spent_on][]=$to"
                        + "&key=$key"
                ).paginatedGet("time_entries")
            .apply {
                // fetch missing issues, and add them to loadedIssues
                val loadedIssuesIds = loadedIssues.map { it.id } // as variable to avoid calculating each iteration...does kotlin simplify this?
                newIssues += downloadIssues(this.map { getIssueId(it) }.distinct().filter { it !in loadedIssuesIds })
                // also initialize user id if not still
                if (userId == null && isNotEmpty()) userId = first().getJSONObject("user").getInt("id")
            }.map { TimeEntry(it, loadedIssues + newIssues, this) }

        return entries to newIssues
    }

    /* ------------------------- issues ------------------------- */

    /**
     * Returns the issues associated with the specified ids
     *
     * @param ids list of ids to retrieve
     * @return the list of issues (maybe less if some are not found!)
     * @throws IOException on network error
     */
    @Throws(IOException::class)
    fun downloadIssues(ids: List<Int>) = ids
        // return empty if no issues
        .ifEmpty { return emptyList<Issue>() }
        .run {
            // build url
            ("${domain}issues.json?utf8=✓"
                    + "&f[]=issue_id&op[issue_id]=%3D&v[issue_id][]=${joinToString("%2C")}"
                    + "&key=$key")
                // get
                .paginatedGet("issues")
                // create issues
                .map { Issue(it, this@Connector) }
        }

    /**
     * Return assigned issues
     */
    @Throws(IOException::class)
    fun downloadAssignedIssues() = (
            "${domain}issues.json?utf8=✓"
                    + "&f[]=assigned_to_id&op[assigned_to_id]=%3D&v[assigned_to_id][]=me"
                    + "&f[]=updated_on&op[updated_on]=>t-&v[updated_on][]=31"
                    + "&key=$key"
            ).paginatedGet("issues")
        .apply {
            // also initialize user id if not still
            if (userId == null && isNotEmpty()) userId = first().getJSONObject("assigned_to").getInt("id")
        }
        .map { Issue(it, this) }

}

/* ------------------------- private ------------------------- */

/**
 * returns all entries from a paginated result
 */
@Throws(IOException::class)
private fun String.paginatedGet(key: String) =
    ArrayList<JSONObject>().apply {
        doWhile {
            // get page
            URL("${this@paginatedGet}&limit=100&offset=$size").getJSON()
                // add to returned list
                .also { addAll(it.getJSONArray(key).mapAsObjects()) }
                // continue to next page if still not all
                .getInt("total_count") > size
        }
    }

/**
 * Map a JSONArray as a list of JSONObject
 */
private fun JSONArray.mapAsObjects() = List(length()) { i -> getJSONObject(i) }

/**
 * Run while it returns false (compact while)
 */
private fun doWhile(thing: () -> Boolean) {
    while (thing()) {
        // nothing
    }
}