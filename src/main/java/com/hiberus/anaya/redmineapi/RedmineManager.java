package com.hiberus.anaya.redmineapi;

import com.hiberus.anaya.redmineeditor.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Redmine API.
 * The 'official' one is not used because it doesn't allow searching with multiple filters
 */
public class RedmineManager {
    private final String domain;
    private final String key;
    private String user = "me";

    public RedmineManager(String domain, String key) {
        this.domain = domain;
        this.key = key;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns all the hours
     *
     * @param from from this date (included)
     * @param to   to this date (included)
     * @return the JSON data
     * @throws IOException if network failed
     */
    public JSONArray getHourEntries(LocalDate from, LocalDate to) throws IOException {
        int offset = 0;
        JSONArray allEntries = new JSONArray();
        int total_count;
        do {
            // get page
            JSONObject pageEntries = JSONUtils.getFromUrl(domain + "time_entries.json?utf8=✓&"
                    + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + user
                    + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=" + from.toString() + "&v[spent_on][]=" + to.toString()
                    + "&key=" + key
                    + "&limit=100&offset=" + offset
            );
            allEntries.putAll(pageEntries.getJSONArray("time_entries"));
            offset = allEntries.length();
            // repeat if still not all
            total_count = pageEntries.getInt("total_count");
        } while (offset < total_count);

        return allEntries;
    }

    public boolean uploadTimeEntry(int id, JSONObject entry) {
        if (id == -1) {
            if (!entry.isEmpty()) {
                // create
                return JSONUtils.postToUrl(domain + "time_entries.json?key=" + key, new JSONObject().put("time_entry", entry)) == 201;
            } else {
                // ignore
                return true;
            }
        } else {
            if (!entry.isEmpty()) {
                // update
                return JSONUtils.putToUrl(domain + "time_entries/" + id + ".json?key=" + key, new JSONObject().put("time_entry", entry)) == 204;
            } else {
                // delete
                return JSONUtils.delete(domain + "time_entries/" + id + ".json?key=" + key) == 204;
            }
        }
    }
}
