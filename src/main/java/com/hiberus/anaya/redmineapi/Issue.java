package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

import java.io.IOException;

/**
 * A redmine issue
 */
public final class Issue {

    /* ------------------------- manager ------------------------- */

    private final RedmineManager manager; // an issue is associated to a manager

    /* ------------------------- data ------------------------- */

    /**
     * The identifier
     */
    public final int id;
    /**
     * The name of its project
     */
    public final String project;
    /**
     * The subject (title)
     */
    public final String subject;
    /**
     * The description, may probably be empty
     */
    public final String description;

    private double estimated_hours; // estimated hours
    private int done_ratio; // realization percentage
    private double spentHours; // total hours, uninitialized

    private final JSONObject original; // the original raw object, for diff purposes

    /* ------------------------- constructors ------------------------- */

    Issue(JSONObject rawIssue, RedmineManager manager) {
        // parse issue from raw json data
        this.manager = manager;
        original = rawIssue;

        id = rawIssue.getInt("id");
        project = rawIssue.getJSONObject("project").optString("name", "");
        subject = rawIssue.optString("subject", "");
        description = rawIssue.optString("description");
        estimated_hours = rawIssue.optDouble("estimated_hours", RedmineManager.NONE);
        done_ratio = rawIssue.optInt("done_ratio", 0);
        spentHours = rawIssue.optDouble("spent_hours", RedmineManager.UNINITIALIZED);
    }

    /* ------------------------- properties getters ------------------------- */

    /**
     * Get total hours spent on this issue.
     * Can be not initialized
     */
    public double getSpent() {
        return spentHours;
    }

    /**
     * Estimated hours
     */
    public double getEstimated() {
        return estimated_hours;
    }

    /**
     * Ratio of realization
     */
    public int getRealization() {
        return done_ratio;
    }

    /**
     * @return the url to see this issue details
     */
    public String getUrl() {
        return manager.domain + "issues/" + id;
    }

    /**
     * @return a short string describing this issue
     * @see #toString()
     */
    public String toShortString() {
        return "#" + id + ": " + subject;
    }

    /**
     * @return a fairly long string describing this issue
     * @see #toShortString()
     */
    @Override
    public String toString() {
        return project + "\n" + toShortString();
    }

    /* ------------------------- properties setters ------------------------- */

    /**
     * Changes the total hours spent on this issue
     *
     * @param amount number oh hours (negative to substract)
     */
    public void addSpent(double amount) {
        assert spentHours >= 0;
        spentHours += amount;
        assert spentHours >= 0;
    }

    public void addEstimated(double amount) {
        // still uninitialized, cancel
        if (estimated_hours == RedmineManager.UNINITIALIZED) return;

        if (estimated_hours == 0 && amount < 0) {
            // no hours and want to subtract, disable
            estimated_hours = RedmineManager.NONE;
            return;
        }
        if (estimated_hours == RedmineManager.NONE) {
            // disabled and want to change, set to 0 if it wants to add, keep if not
            if (amount > 0) estimated_hours = 0;
            return;
        }

        // else, change
        amount = Math.max(estimated_hours + amount, 0) - estimated_hours; // don't subtract what can't be subtracted
        estimated_hours += amount;
    }

    public void addRealization(int amount) {
        amount = clamp(0, done_ratio + amount, 100) - done_ratio; // don't subtract what can't be subtracted, and don't add what can be added
        done_ratio += amount;
    }

    public void syncRealization() {
        done_ratio = clamp(0, (int) (spentHours / estimated_hours * 100), 100);
    }

    /* ------------------------- downloading ------------------------- */

    /**
     * Loads spent hours (nothing if already initialized)
     * Long operation
     *
     * @throws IOException on network error
     */
    public void downloadSpent() throws IOException {
        if (spentHours == RedmineManager.UNINITIALIZED) {
            spentHours = UrlJSONKt.get(manager.domain + "issues/" + id + ".json?key=" + manager.key)
                    .getJSONObject("issue").optDouble("spent_hours", RedmineManager.NONE);
        }
    }

    /* ------------------------- uploading ------------------------- */

    /**
     * @return a list of changes made to this issue, as an object (empty means no changes)
     */
    public JSONObject getChanges() {
        JSONObject changes = new JSONObject();

        // TODO: use a JSON as container so this can be a simple foreach diff
        if (original == null || estimated_hours != original.optDouble("estimated_hours", RedmineManager.NONE)) {
            // changed hours
            changes.put("estimated_hours", estimated_hours == RedmineManager.NONE ? "" : estimated_hours);
        }
        if (original == null || done_ratio != original.getDouble("done_ratio")) {
            // changed comment
            changes.put("done_ratio", done_ratio);
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

        // update
        System.out.println("Updating issue " + id + " with data: " + changes);
        if (RedmineManager.OFFLINE) return;
        if (UrlJSONKt.put(manager.domain + "issues/" + id + ".json?key=" + manager.key, new JSONObject().put("issue", changes)) != 200) {
            throw new IOException("Error when updating issue " + id + " with data: " + changes);
        }
    }

    /* ------------------------- utils ------------------------- */

    private static int clamp(int min, int value, int max) {
        return Math.min(Math.max(value, min), max);
    }
}
