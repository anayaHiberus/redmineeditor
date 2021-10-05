package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

import java.io.IOException;

/**
 * A redmine issue
 */
public final class Issue {

    /* ------------------------- manager ------------------------- */

    private final RedmineManager manager;

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

    public double spent_hours;

    /* ------------------------- constructors ------------------------- */

    Issue(JSONObject rawIssue, RedmineManager manager) {
        this.manager = manager;
        // parse from raw JSON
        id = rawIssue.getInt("id");
        project = rawIssue.getJSONObject("project").optString("name", "");
        subject = rawIssue.optString("subject", "");
        description = rawIssue.optString("description");
        estimated_hours = rawIssue.optDouble("estimated_hours", -1);
        done_ratio = rawIssue.optInt("done_ratio", 0);
        spent_hours = rawIssue.optDouble("spent_hours", -2);
    }

    /* ------------------------- properties ------------------------- */

    public void fill() throws IOException {
        if (spent_hours == -2) {
            spent_hours = UrlJSON.get(manager.domain + "issues/" + id + ".json?key=" + manager.key).getJSONObject("issue").optDouble("spent_hours", -1);
        }
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
