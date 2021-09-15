package com.hiberus.anaya.redmineeditor.utils.hiberus;

import com.hiberus.anaya.redmineeditor.utils.JsonReader;
import org.json.JSONArray;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Redmine API...check the existing one too!
 */
public class Redmine {

    public static final String URL = "https://redmine.hiberus.com/redmine/"; // redmine url
    public static final String KEY = "89c99d1d6adbfebbd08d8e7960b15f282159dccc"; // redmine key

    /**
     * Returns all the hours
     *
     * @param user of this user
     * @param from from this date (included)
     * @param to   to this date (included)
     * @return the JSON data
     * @throws IOException if network failed
     */
    public static JSONArray getHourEntries(String user, LocalDate from, LocalDate to) throws IOException {
        return JsonReader.fromUrl(URL + "time_entries.json?utf8=✓&"
                + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + user
                + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=" + from.toString() + "&v[spent_on][]=" + to.toString()
                + "&key=" + KEY
        ).getJSONArray("time_entries");
    }
}
