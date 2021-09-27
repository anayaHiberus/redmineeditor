package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 'You spent X hours into an issue with a message' object
 */
public class TimeEntry {
    final int id; // the entry id in the database
    /**
     * The issue id
     */
    public final Issue issue;
    private final LocalDate spent_on; // date it was spent
    private double hours = 0; // hours spent
    private String comment = ""; // comment

    private JSONObject original; // the original raw object, for diff purposes

    TimeEntry(JSONObject rawEntry, List<Issue> issues) {
        // creates a new entry from a json raw data
        original = rawEntry;
        id = rawEntry.getInt("id");
        issue = issues.stream().filter(issue -> issue.id == getIssueId(rawEntry)).findFirst().orElseThrow();
        spent_on = LocalDate.parse(rawEntry.getString("spent_on"));
        hours = rawEntry.getDouble("hours");
        comment = rawEntry.optString("comments");
    }

    static int getIssueId(JSONObject rawEntry) {
        return rawEntry.getJSONObject("issue").getInt("id");
    }

    /**
     * Creates a new time entry for an existing issue and date
     *
     * @param issue    the issue id this entry is spent on
     * @param spent_on the date this entry is spent on
     */
    public TimeEntry(Issue issue, LocalDate spent_on) {
        this.id = -1;
        this.issue = issue;
        this.spent_on = spent_on;
    }

    /**
     * Checks if this entry was spent on a specific date
     *
     * @param date check spent with this date
     * @return true if this entry was spent on that date
     */
    public boolean wasSpentOn(LocalDate date) {
        return spent_on.equals(date);
    }

    /**
     * @return the spent hours of this entry
     */
    public double getHours() {
        return hours;
    }

    /**
     * @return the comment of this entry
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment new comment of this entry
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @param amount new hours to add to this entry (negative to subtract)
     */
    public void changeHours(double amount) {
        hours = Math.max(hours + amount, 0); // change, but keep positive
    }

    /**
     * @return a list of changes made to this entry, as an object (empty means no changes)
     */
    public JSONObject getChanges() {
        JSONObject changes = new JSONObject();

        if (original == null || hours != original.getDouble("hours")) {
            // changed hours
            changes.put("hours", hours);
        }
        if (original == null || !Objects.equals(comment, original.optString("comments"))) {
            // changed comment
            changes.put("comments", comment);
        }
        if (id == -1) {
            // without original, this data is considered new
            changes.put("issue_id", issue.id);
            changes.put("spent_on", spent_on);
        }

        // return changes (empty for nothing)
        return changes;
    }

    /**
     * @return true if this entry requires upload, false otherwise
     */
    public boolean requiresUpload() {
        return !(
                getChanges().isEmpty() // no changes, no upload
                        ||
                        this.id == -1 && getHours() <= 0 // no useful changes, no upload
        );
    }
}
