package com.hiberus.anaya.redmineapi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redmine API.
 * The 'official' one is not used because it doesn't allow searching with multiple filters
 */
public class RedmineManager {

    /**
     * Unset int property
     */
    public static final int NONE = -1;
    /**
     * Not initialized int property
     */
    public static final int UNINITIALIZED = -2;

    /* ------------------------- params ------------------------- */
    static final boolean OFFLINE = false; // for debug purpose, set to true to disable changes

    // configurable
    final String domain; // the redmine domain
    final String key; // the api key

    /* ------------------------- init ------------------------- */

    /**
     * Initializes a manager
     *
     * @param domain the redmine domain
     * @param key    the redmine api key
     */
    public RedmineManager(String domain, String key) {
        this.domain = domain;
        this.key = key;
    }

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
    public List<TimeEntry> getTimeEntries(LocalDate from, LocalDate to, List<Issue> loadedIssues) throws IOException {
        List<JSONObject> time_entries = paginatedGet(
                domain + "time_entries.json?utf8=✓&"
                        + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + "me" // you can only edit your own entries, so 'me' is the only useful value
                        + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=" + from.toString() + "&v[spent_on][]=" + to.toString()
                        + "&key=" + key,
                "time_entries");

        // fetch missing issues
        List<Integer> loadedIssuesIds = loadedIssues.stream().map(issue -> issue.id).toList();
        loadedIssues.addAll(getIssues(time_entries.stream()
                .map(TimeEntry::getIssueId)
                .distinct()
                .filter(o -> !loadedIssuesIds.contains(o))
                .toList()
        ));
        return time_entries
                .stream().map(rawEntry -> new TimeEntry(rawEntry, loadedIssues, this)).toList();
    }

    /**
     * Creates a new Time Entry associated with this manager
     *
     * @param issue    issue for the entry
     * @param spent_on day this entry is spent on
     * @return the created entry
     */
    public TimeEntry newTimeEntry(Issue issue, LocalDate spent_on) {
        return new TimeEntry(issue, spent_on, this);
    }

    /* ------------------------- issues ------------------------- */

    /**
     * Returns the issues associated with the specified ids
     *
     * @param ids list of ids to retrieve
     * @return the list of issues (may be less if some are not found!
     * @throws IOException on network error
     */
    public List<Issue> getIssues(List<Integer> ids) throws IOException {
        if (ids.isEmpty()) return Collections.emptyList();

        String idsString = ids.stream().map(id -> Integer.toString(id)).collect(Collectors.joining("%2C"));
        return paginatedGet(
                domain + "issues.json?utf8=✓&"
                        + "&f[]=issue_id&op[issue_id]=%3D&v[issue_id][]=" + idsString
                        + "&key=" + key,
                "issues")
                .stream().map(rawIssue -> new Issue(rawIssue, this)).toList();
    }

    /* ------------------------- private ------------------------- */

    private List<JSONObject> paginatedGet(String fullUrl, String key) throws IOException {
        // returns all entries from a paginated result
        int offset = 0;
        List<JSONObject> allObjects = new ArrayList<>();
        int total_count;
        do {
            // get page
            JSONObject page = UrlJSON.get(fullUrl
                    + "&limit=100&offset=" + offset
            );

            // add page
            JSONArray pageObjects = page.getJSONArray(key);
            for (int i = 0; i < pageObjects.length(); i++) {
                allObjects.add(pageObjects.getJSONObject(i));
            }
            offset = allObjects.size();

            // continue next page if still not all
            total_count = page.getInt("total_count");
        } while (offset < total_count);

        return allObjects;
    }
}
