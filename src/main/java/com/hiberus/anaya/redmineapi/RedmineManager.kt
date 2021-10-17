package com.hiberus.anaya.redmineapi

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.time.LocalDate

/* ------------------------- defaults ------------------------- */

/**
 * Unset int property
 */
const val iNONE = -1

/**
 * Check if the value is set (not NONE)
 */
val Int.isSet get() = this >= 0

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

/**
 * for debug purposes, set to true to disable online PUT/POST petitions
 */
const val OFFLINE = false

/* ------------------------- class ------------------------- */

/**
 * Redmine API.
 * The 'official' one is not used because it doesn't allow searching with multiple filters
 * @param domain the redmine domain
 * @param key the redmine api key
 */
class RedmineManager(
    val domain: String,
    val key: String,
) {

    /* ------------------------- properties ------------------------- */

    fun buildUrl(path: String) = URL("$domain$path.json?key=$key")

    /* ------------------------- time entries ------------------------- */

    /**
     * Returns all the entries on a timeframe
     *
     * @param from         from this date (included)
     * @param to           to this date (included)
     * @param loadedIssues issues already loaded. New ones will be added here!!!
     * @return the list of entries from that data
     * @throws IOException if network failed
     */
    @Throws(IOException::class)
    fun getTimeEntries(from: LocalDate, to: LocalDate, loadedIssues: MutableList<Issue>) = (
            "${domain}time_entries.json?utf8=✓&"
                    + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=${"me"}" // you can only edit your own entries, so 'me' is the only useful value
                    + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=$from&v[spent_on][]=$to"
                    + "&key=$key"
            ).paginatedGet("time_entries")
        .apply {
            // fetch missing issues, and add them to loadedIssues
            val loadedIssuesIds = loadedIssues.map { it.id }.distinct() // as variable to avoid calculating each iteration...does kotlin simplify this?
            getIssues(this.map { getIssueId(it) }.distinct().filter { it !in loadedIssuesIds }).toCollection(loadedIssues)
        }.map { TimeEntry(it, loadedIssues, this) }

    /**
     * Creates a new Time Entry associated with this manager
     *
     * @param issue    issue for the entry
     * @param spent_on day this entry is spent on
     * @return the created entry
     */
    fun newTimeEntry(issue: Issue, spent_on: LocalDate) = TimeEntry(issue, spent_on, this)

    /* ------------------------- issues ------------------------- */

    /**
     * Returns the issues associated with the specified ids
     *
     * @param ids list of ids to retrieve
     * @return the list of issues (maybe less if some are not found!)
     * @throws IOException on network error
     */
    @Throws(IOException::class)
    fun getIssues(ids: List<Int>) = ids
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
                .map { Issue(it, this@RedmineManager) }
        }


}

/* ------------------------- private ------------------------- */

/**
 * returns all entries from a paginated result
 */
@Throws(IOException::class)
private fun String.paginatedGet(key: String): List<JSONObject> =
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
private fun JSONArray.mapAsObjects(): List<JSONObject> = List(length()) { i -> getJSONObject(i) }

/**
 * Run while it returns false (compact while)
 */
private fun doWhile(thing: () -> Boolean) {
    while (thing()) {
        // nothing
    }
}