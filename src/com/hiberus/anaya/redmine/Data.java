package com.hiberus.anaya.redmine;

import com.hiberus.anaya.utils.IsoDate;
import com.hiberus.anaya.utils.JsonReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class Data {

    private JSONArray entries = new JSONArray();

    public void loadEntries(String user, Calendar from, Calendar to) {
        try {
            entries = JsonReader.fromUrl("https://redmine.hiberus.com/redmine/time_entries.json?utf8=✓&"
                    + "&f[]=user_id&op[user_id]=%3D&v[user_id][]=" + user
                    + "&f[]=spent_on&op[spent_on]=><&v[spent_on][]=" + IsoDate.format(from) + "&v[spent_on][]=" + IsoDate.format(to)
                    + "&key=89c99d1d6adbfebbd08d8e7960b15f282159dccc"
            ).getJSONArray("time_entries");
        } catch (IOException e) {
            e.printStackTrace();
            entries = new JSONArray();
        }
    }

    public double getSpent(Calendar from) {
        double spent = 0;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (Objects.equals(entry.getString("spent_on"), IsoDate.format(from)))
                spent += entry.getDouble("hours");
        }
        return spent;
    }
}
