package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.time.LocalDate

/* ------------------------- class ------------------------- */
/**
 * Manages connections with the api on a [domain] using an api [key].
 * If [read_only], put/post petitions will be skipped (but still logged)
 */
internal class Remote(
    private val domain: String,
    private val key: String,
    private val read_only: Boolean,
) {

    /* ------------------------- Endpoint builder ------------------------- */

    /**
     * Implemented API endpoints
     */
    private enum class Endpoint {
        TIME_ENTRIES,
        ISSUES,
    }

    /**
     * url parameter: [field] [operation] [values]/[value]
     */
    private data class Param(val field: String, val operation: String, val values: List<Any>) {
        constructor(field: String, operation: String, value: Any) : this(field, operation, listOf(value))
    }

    /**
     * The url of this endpoint, from a specific [id] if not empty, and with a list of [parameters] if supplied (set to null for user url)
     */
    private fun Endpoint.build(id: Int? = null, parameters: List<Param>? = listOf()) = buildString {
        // domain
        append(domain.removeSuffix("/")).append("/")
        // entry
        append(name.lowercase())
        // id if supplied
        if (id != null) append("/").append(id)

        // nothing else if parameters is null
        if (parameters == null) return@buildString

        // file
        append(".json?")
        // parameters
        if (parameters.isNotEmpty()) append("utf8=✓&")
        parameters.forEach { (field, operation, values) ->
            // field
            append("f[]=$field&")
            // operation
            val op = when (operation) {
                "=" -> "%3D"
                "between" -> "><"
                else -> operation
            }
            append("op[$field]=$op&")
            // values
            assert(values.isNotEmpty())
            values.forEach { append("v[$field][]=$it&") }
        }
        // key
        append("key=$key")
    }.also { println(it) }

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
        val entries = Endpoint.TIME_ENTRIES.build(parameters = listOf(
            Param("user_id", "=", "me"),
            Param("spent_on", "between", listOf(from, to))
        )).paginatedGet("time_entries")
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
                            .postTo(Endpoint.TIME_ENTRIES.build().url)
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
                            .putTo(Endpoint.TIME_ENTRIES.build(id = id).url)
                            .ifNot(200) {
                                throw IOException("Error when updating entry $id with data: $it")
                            }
                    }

                    // existing entry without hours, delete
                    id != null && spent <= 0 -> {
                        println("Deleting entry $id")
                        if (read_only) return
                        Endpoint.TIME_ENTRIES.build(id = id).url
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
    fun getIssueDetailsUrl(issue: Issue) = Endpoint.ISSUES.build(id = issue.id, parameters = null)

    /**
     * Returns the raw details of an issue, for internal use
     */
    @Throws(IOException::class)
    internal fun downloadRawIssueDetails(id: Int) = Endpoint.ISSUES.build(id = id).url.getJSON().getJSONObject("issue")

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
            Endpoint.ISSUES.build(parameters = listOf(
                Param("issue_id", "=", joinToString("%2C"))
            ))
                // get
                .paginatedGet("issues")
                // create issues
                .map { Issue(it, this@Remote) }
        }

    /**
     * Return assigned issues
     */
    @Throws(IOException::class)
    fun downloadAssignedIssues() =
        Endpoint.ISSUES.build(parameters = listOf(
            Param("assigned_to_id", "=", "me"),
            Param("updated_on", "t-", "31")
        )).paginatedGet("issues")
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
                    .putTo(Endpoint.ISSUES.build(id = id).url)
                    .ifNot(200) { throw IOException("Error when updating issue $id with data: $it") }
            }
        }
    }
}


/* ------------------------- private ------------------------- */

/**
 * Functional way to convert a string to an url: "http".url instead of URL("http")
 */
private inline val String.url get() = URL(this)

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
private inline fun doWhile(thing: () -> Boolean) {
    while (thing()) {
        // nothing
    }
}