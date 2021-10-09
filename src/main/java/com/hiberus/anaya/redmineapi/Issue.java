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
    /**
     * Estimated hours
     */
    public final double estimated_hours;

    /**
     * Ratio of realization
     */
    public final int done_ratio;

    private double spentHours; // total hours, uninitialized

    /* ------------------------- constructors ------------------------- */

    Issue(JSONObject rawIssue, RedmineManager manager) {
        this.manager = manager;
        // parse from raw JSON
        id = rawIssue.getInt("id");
        project = rawIssue.getJSONObject("project").optString("name", "");
        subject = rawIssue.optString("subject", "");
        description = rawIssue.optString("description");
        estimated_hours = rawIssue.optDouble("estimated_hours", RedmineManager.NONE);
        done_ratio = rawIssue.optInt("done_ratio", 0);
        spentHours = rawIssue.optDouble("spent_hours", RedmineManager.UNINITIALIZED);
    }

    /* ------------------------- properties ------------------------- */

    /**
     * Loads spent hours (nothing if already initialized)
     * Long operation
     *
     * @throws IOException on network error
     */
    public void loadSpent() throws IOException {
        if (spentHours == RedmineManager.UNINITIALIZED) {
            spentHours = UrlJSON.get(manager.domain + "issues/" + id + ".json?key=" + manager.key)
                    .getJSONObject("issue").optDouble("spent_hours", RedmineManager.NONE);
        }
    }

    /**
     * Get total hours spent on this issue.
     * Can be not initialized
     */
    public double getSpentHours() {
        return spentHours;
    }

    /**
     * Changes the total hours spent on this issue
     *
     * @param amount number oh hours (negative to substract)
     */
    public void addSpentHours(double amount) {
        assert spentHours >= 0;
        spentHours += amount;
        assert spentHours >= 0;
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

    /**
     * @return the url to see this issue details
     */
    public String getUrl() {
        return manager.domain + "issues/" + id;
    }
}
