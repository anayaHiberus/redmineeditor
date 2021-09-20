package com.hiberus.anaya.redmineapi;

import com.hiberus.anaya.redmineeditor.utils.JSONUtils;
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
    // static
    static private final String USER = "me";
    static private final boolean OFFLINE = false; // for debug purpose, set to true to disable changes

    // configurable
    private final String domain;
    private final String key;

    public RedmineManager(String domain, String key) {
        this.domain = domain;
        this.key = key;
    }

    /**
     * Returns all the hours
     *
     * @param from from this date (included)
     * @param to   to this date (included)
     * @return the JSON data
     * @throws IOException if network failed
     */
    public List<TimeEntry> getHourEntries(LocalDate from, LocalDate to) throws IOException {
        int offset = 0;
        List<TimeEntry> allEntries = new ArrayList<>();
        int total_count;
        do {
            // get page
            JSONObject page = JSONUtils.getFromUrl(domain + "time_entries.json?utf8=✓&"
                    + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + USER
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

            // repeat if still not all
            total_count = page.getInt("total_count");
        } while (offset < total_count);

        return allEntries;
    }

    public boolean uploadTimeEntry(TimeEntry entry) {

        int id = entry.id;

        // get changes
        JSONObject changes = entry.getChanges();

        // ignore unmodified
        if (changes.isEmpty()) return true;

        if (id == -1) {
            if (entry.getHours() > 0) {
                // create
                System.out.println("Creating entry with data: " + changes);
                return OFFLINE || JSONUtils.postToUrl(domain + "time_entries.json?key=" + key, new JSONObject().put("time_entry", changes)) == 201;
            } else {
                // ignore
                return true;
            }
        } else {
            if (entry.getHours() > 0) {
                // update
                System.out.println("Updating entry " + id + " with data: " + changes);
                return OFFLINE || JSONUtils.putToUrl(domain + "time_entries/" + id + ".json?key=" + key, new JSONObject().put("time_entry", changes)) == 200;
            } else {
                // delete
                System.out.println("Deleting entry " + id);
                return OFFLINE || JSONUtils.delete(domain + "time_entries/" + id + ".json?key=" + key) == 200;
            }
        }
    }
}
