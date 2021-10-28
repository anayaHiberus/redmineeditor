package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.time.LocalDate

/* ------------------------- class ------------------------- */

/**
 * Manages connections with the api on a [domain] (MUST NOT END WITH A SLASH) using an api [key].
 * If [read_only], put/post petitions will be skipped (but still logged)
 */
internal class Remote(
    private val domain: String,
    private val key: String,
    private val read_only: Boolean,
) {

    // TODO: consider extracting urls for common use

    /* ------------------------- properties ------------------------- */

    /**
     * Loaded id of the user (null if uninitialized)
     * TODO: consider using users/current.json, and display its info in a new component. maybe even use this field to detect if the api is ok or not
     */
    var userId: Int? = null

    /* ------------------------- time entries ------------------------- */

    /**
     * Returns all the entries on a timeframe for the current user
     * TODO: if the api is wrong, this returns an empty list!
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
                "$domain/time_entries.json?utf8=✓&"
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

    /**
     * Uploads an [entry] to redmine (unless not needed)
     *
     * @throws IOException on upload error
     */
    @Throws(IOException::class)
    fun upload(entry: TimeEntry) {
        entry.run {
            changes.takeUnless { it.isEmpty }?.also { // if there are changes
                when {
                    // new entry with hours, create
                    id == null && spent > 0 -> {
                        println("Creating entry with data: $it")
                        if (read_only) return
                        JSONObject().put("time_entry", it)
                            .postTo(URL("$domain/time_entries.json?key=$key"))
                            .ifNot(201) {
                                throw IOException("Error when creating entry with data: $it")
                            }
                    }

                    // new entry without hours, ignore
                    id == null && spent <= 0 -> Unit

                    // existing entry with hours, update
                    id != null && spent > 0 -> {
                        println("Updating entry $id with data: $it")
                        if (read_only) return
                        JSONObject().put("time_entry", it)
                            .putTo(URL("$domain/time_entries/$id.json?key=$key"))
                            .ifNot(200) {
                                throw IOException("Error when updating entry $id with data: $it")
                            }
                    }

                    // existing entry without hours, delete
                    id != null && spent <= 0 -> {
                        println("Deleting entry $id")
                        if (read_only) return
                        URL("$domain/time_entries/$id.json?key=$key")
                            .delete()
                            .ifNot(200) {
                                throw IOException("Error when deleting entry $id")
                            }
                    }

                    // should never happen, but if it does, do nothing
                    else -> println("This should never happen! $id $spent")
                }
            }
        }
    }

    /* ------------------------- issues ------------------------- */

    /**
     * returns the url to externally view this issue details
     */
    fun getIssueDetailsUrl(issue: Issue) = "$domain/issues/${issue.id}"

    /**
     * Returns the raw details of an issue, for internal use
     */
    @Throws(IOException::class)
    internal fun downloadRawIssueDetails(id: Int) = URL("$domain/issues/$id.json?key=$key").getJSON().getJSONObject("issue")

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
            ("$domain/issues.json?utf8=✓"
                    + "&f[]=issue_id&op[issue_id]=%3D&v[issue_id][]=${joinToString("%2C")}"
                    + "&key=$key")
                // get
                .paginatedGet("issues")
                // create issues
                .map { Issue(it, this@Remote) }
        }

    /**
     * Return assigned issues
     */
    @Throws(IOException::class)
    fun downloadAssignedIssues() = (
            "$domain/issues.json?utf8=✓"
                    + "&f[]=assigned_to_id&op[assigned_to_id]=%3D&v[assigned_to_id][]=me"
                    + "&f[]=updated_on&op[updated_on]=>t-&v[updated_on][]=31"
                    + "&key=$key"
            ).paginatedGet("issues")
        .apply {
            // also initialize user id if not still
            if (userId == null && isNotEmpty()) userId = first().getJSONObject("assigned_to").getInt("id")
        }
        .map { Issue(it, this) }

    /**
     * Uploads an [issue] to redmine (unless not needed)
     */
    @Throws(IOException::class)
    fun upload(issue: Issue) {
        issue.run {
            changes.takeUnless { it.isEmpty }?.let { // if there are changes
                // update
                println("Updating issue $id with data: $it")
                if (read_only) return
                JSONObject().put("issue", this)
                    .putTo(URL("$domain/issues/$id.json?key=$key"))
                    .ifNot(200) { throw IOException("Error when updating issue $id with data: $it") }
            }
        }
    }
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
 * Run while it returns false (compact while)
 */
private fun doWhile(thing: () -> Boolean) {
    while (thing()) {
        // nothing
    }
}