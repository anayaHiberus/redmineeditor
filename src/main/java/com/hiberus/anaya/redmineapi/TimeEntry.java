package com.hiberus.anaya.redmineapi;

import com.hiberus.anaya.redmineeditor.utils.ObservableProperty;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Objects;

public class TimeEntry extends ObservableProperty.Property {
    public final int id;
    public final int issue;
    private final LocalDate spent_on;
    private double hours = 0;
    private String comment = "";

    private JSONObject original;

    public TimeEntry(JSONObject entry) {
        this.original = entry;
        id = entry.getInt("id");
        issue = entry.getJSONObject("issue").getInt("id");
        spent_on = LocalDate.parse(entry.getString("spent_on"));
        hours = entry.getDouble("hours");
        comment = entry.optString("comments");
    }

    public TimeEntry(Integer issue, LocalDate spent_on) {
        this.id = -1;
        this.issue = issue;
        this.spent_on = spent_on;
    }

    public boolean wasSpentOn(LocalDate date) {
        return spent_on.equals(date);
    }

    public double getHours() {
        return hours;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void changeHours(double amount) {
        double newHours = hours + amount;
        if (newHours >= 0) {
            hours = newHours;
            notifyChanged();
        }
    }

    public JSONObject getChanges() {
        JSONObject changes = new JSONObject();

        // no hours, return empty (means delete or ignore)
        if (hours == 0) return changes;

        if (original == null || hours != original.getDouble("hours")) {
            changes.put("hours", hours);
        }
        if (original == null || !Objects.equals(comment, original.optString("comments"))) {
            changes.put("comments", comment);
        }
        if (id == -1) {
            changes.put("issue_id", issue);
            changes.put("spent_on", spent_on);
        }

        // return changes (or null for nothing)
        if (changes.isEmpty()) return null;
        else return changes;
    }
}
