package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

/**
 * A redmine issue
 */
public final class Issue {
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

    Issue(JSONObject rawIssue) {
        // parse from raw JSON
        id = rawIssue.getInt("id");
        project = rawIssue.getJSONObject("project").optString("name", "");
        subject = rawIssue.optString("subject", "");
        description = rawIssue.optString("description");
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
}
