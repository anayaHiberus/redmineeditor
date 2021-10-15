package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * 'You spent X hours into an issue with a message' object
 */
public class TimeEntry {

    /* ------------------------- manager ------------------------- */

    private final RedmineManager manager; // an entry is associated to a manager

    static int getIssueId(JSONObject rawEntry) {
        // returns the id from a rawEntry
        return rawEntry.getJSONObject("issue").getInt("id");
    }

    /* ------------------------- data ------------------------- */

    final int id; // the entry id in the database
    /**
     * The issue id
     */
    public final Issue issue;
    private final LocalDate spent_on; // date it was spent
    private double spent = 0; // hours spent
    private String comment = ""; // comment

    private JSONObject original; // the original raw object, for diff purposes

    /* ------------------------- constructors ------------------------- */

    TimeEntry(JSONObject rawEntry, List<Issue> issues, RedmineManager manager) {
        // creates a new entry from a json raw data
        this.manager = manager;
        original = rawEntry;

        id = rawEntry.getInt("id");
        issue = issues.stream().filter(issue -> issue.id == getIssueId(rawEntry)).findFirst().orElseThrow();
        spent_on = LocalDate.parse(rawEntry.getString("spent_on"));
        spent = rawEntry.getDouble("hours");
        comment = rawEntry.optString("comments");
    }

    TimeEntry(Issue issue, LocalDate spent_on, RedmineManager manager) {
        // Creates a new time entry for an existing issue and date
        this.manager = manager;
        this.id = RedmineManager.NONE;
        this.issue = issue;
        this.spent_on = spent_on;
    }

    /* ------------------------- properties getters ------------------------- */

    /**
     * Checks if this entry was spent on a specific date
     *
     * @param date check spent with this date
     * @return true iff this entry was spent on that date
     */
    public boolean wasSpentOn(LocalDate date) {
        return spent_on.equals(date);
    }

    /**
     * Checks if this entry was spent on a specific month
     *
     * @param month check spent with this month
     * @return true iff this entry was spent on that month
     */
    public boolean wasSpentOn(YearMonth month) {
        return YearMonth.from(spent_on).equals(month);
    }


    /**
     * @return the spent hours of this entry
     */
    public double getSpent() {
        return spent;
    }

    /**
     * @return the comment of this entry
     */
    public String getComment() {
        return comment;
    }

    /* ------------------------- properties setters ------------------------- */

    /**
     * @param comment new comment of this entry
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @param amount new hours to add to this entry (negative to subtract)
     */
    public void addSpent(double amount) {
        amount = Math.max(spent + amount, 0) - spent; // don't subtract what can't be substracted
        spent += amount;
        issue.addSpent(amount);
        assert issue.getSpent() >= 0;
    }

    /* ------------------------- uploading ------------------------- */

    /**
     * @return a list of changes made to this entry, as an object (empty means no changes)
     */
    public JSONObject getChanges() {
        JSONObject changes = new JSONObject();

        if (original == null || spent != original.getDouble("hours")) {
            // changed hours
            changes.put("hours", spent);
        }
        if (original == null || !Objects.equals(comment, original.optString("comments"))) {
            // changed comment
            changes.put("comments", comment);
        }
        if (id == RedmineManager.NONE) {
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
                        (this.id == RedmineManager.NONE && getSpent() <= 0) // no useful changes, no upload
        );
    }

    /**
     * Uploads an entry to redmine (unless not needed)
     *
     * @throws IOException on upload error
     */
    public void uploadTimeEntry() throws IOException {
        // get changes
        JSONObject changes = getChanges();

        // ignore unmodified
        if (changes.isEmpty()) return;

        if (id == RedmineManager.NONE) {
            if (getSpent() > 0) {
                // new entry with hours, create
                System.out.println("Creating entry with data: " + changes);
                if (RedmineManager.OFFLINE) return;
                if (UrlJSONKt.post(manager.domain + "time_entries.json?key=" + manager.key, new JSONObject().put("time_entry", changes)) != 201) {
                    throw new IOException("Error when creating entry with data: " + changes);
                }
            }
            //new entry without hours, ignore
        } else {
            if (getSpent() > 0) {
                // existing entry with hours, update
                System.out.println("Updating entry " + id + " with data: " + changes);
                if (RedmineManager.OFFLINE) return;
                if (UrlJSONKt.put(manager.domain + "time_entries/" + id + ".json?key=" + manager.key, new JSONObject().put("time_entry", changes)) != 200) {
                    throw new IOException("Error when updating entry " + id + " with data: " + changes);
                }
            } else {
                // existing entry without hours, delete
                System.out.println("Deleting entry " + id);
                if (RedmineManager.OFFLINE) return;
                if (UrlJSONKt.delete(manager.domain + "time_entries/" + id + ".json?key=" + manager.key) != 200) {
                    throw new IOException("Error when deleting entry " + id);
                }
            }
        }
    }
}
