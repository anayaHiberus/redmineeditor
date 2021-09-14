package com.hiberus.anaya.model;

import com.hiberus.anaya.utils.hiberus.Redmine;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class Model {

    private String user = "me";

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    //-------------------------------------

    private YearMonth month = YearMonth.now();

    public YearMonth getMonth() {
        return month;
    }

    public void setMonth(YearMonth month) {
        this.month = month;
    }

    //-----------------------------------

    private int day = LocalDate.now().getDayOfMonth(); // if 0, no day selected

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    //----------------------------

    private JSONArray entries = new JSONArray();

    public void loadEntries() {
        try {
            entries = Redmine.getHourEntries(user, month.atDay(1), month.atEndOfMonth());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load entries from Redmine", e);
        }
    }

    public double getSpent(LocalDate day) {
        double spent = 0;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (Objects.equals(entry.getString("spent_on"), day.toString()))
                spent += entry.getDouble("hours");
        }
        return spent;
    }
}
