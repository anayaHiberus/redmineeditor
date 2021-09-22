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
    static private final boolean OFFLINE = false; // for debug purpose, set to true to disable changes

    // configurable
    private final String domain; // the redmine domain
    private final String key; // the api key

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

    /**
     * Returns all the entries on a timeframe
     *
     * @param from from this date (included)
     * @param to   to this date (included)
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
                .filter(o -> !loadedIssuesIds.contains(o))
                .toList()
        ));
        return time_entries
                .stream().map(rawEntry -> new TimeEntry(rawEntry, loadedIssues)).toList();
    }

    /**
     * Uploads an entry to redmine (unless not needed)
     *
     * @param entry entry to upload
     * @throws IOException on upload error
     */
    public void uploadTimeEntry(TimeEntry entry) throws IOException {
        int id = entry.id;

        // get changes
        JSONObject changes = entry.getChanges();

        // ignore unmodified
        if (changes.isEmpty()) return;

        if (id == -1) {
            if (entry.getHours() > 0) {
                // new entry with hours, create
                System.out.println("Creating entry with data: " + changes);
                if (OFFLINE) return;
                if (UrlJSON.post(domain + "time_entries.json?key=" + key, new JSONObject().put("time_entry", changes)) != 201) {
                    throw new IOException("Error when creating entry with data: " + changes);
                }
            }
            //new entry without hours, ignore
        } else {
            if (entry.getHours() > 0) {
                // existing entry with hours, update
                System.out.println("Updating entry " + id + " with data: " + changes);
                if (OFFLINE) return;
                if (UrlJSON.put(domain + "time_entries/" + id + ".json?key=" + key, new JSONObject().put("time_entry", changes)) != 200) {
                    throw new IOException("Error when updating entry " + id + " with data: " + changes);
                }
            } else {
                // existing entry without hours, delete
                System.out.println("Deleting entry " + id);
                if (OFFLINE) return;
                if (UrlJSON.delete(domain + "time_entries/" + id + ".json?key=" + key) != 200) {
                    throw new IOException("Error when deleting entry " + id);
                }
            }
        }
    }

    public List<Issue> getIssues(List<Integer> ids) throws IOException {
        if (ids.isEmpty()) return Collections.emptyList();

        String idsString = ids.stream().map(id -> Integer.toString(id)).collect(Collectors.joining("%2C"));
        return paginatedGet(
                domain + "issues.json?utf8=✓&"
                        + "&f[]=issue_id&op[issue_id]=%3D&v[issue_id][]=" + idsString
                        + "&key=" + key,
                "issues")
                .stream().map(Issue::new).toList();

    }

    // ------------------------- private -------------------------

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
