package com.hiberus.anaya.redmineapi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    public List<TimeEntry> getTimeEntries(LocalDate from, LocalDate to) throws IOException {
        int offset = 0;
        List<TimeEntry> allEntries = new ArrayList<>();
        int total_count;
        do {
            // get page
            JSONObject page = UrlJSON.get(domain + "time_entries.json?utf8=✓&"
                    + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + "me" // you can only edit your own entries, so 'me' is the only useful value
                    + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=" + from.toString() + "&v[spent_on][]=" + to.toString()
                    + "&key=" + key
                    + "&limit=100&offset=" + offset
            );

            // add page
            JSONArray pageEntries = page.getJSONArray("time_entries");
            for (int i = 0; i < pageEntries.length(); i++) {
                allEntries.add(new TimeEntry(pageEntries.getJSONObject(i)));
            }
            offset = allEntries.size();

            // continue next page if still not all
            total_count = page.getInt("total_count");
        } while (offset < total_count);

        return allEntries;
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
}
