package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

public final class Issue {
    public final int id;
    public final String project;
    public final String subject;
    public final String description;

    public Issue(JSONObject rawIssue) {
        id = rawIssue.getInt("id");
        project = rawIssue.getJSONObject("project").optString("name", "");
        subject = rawIssue.optString("subject", "");
        description = rawIssue.optString("description");
    }

    public String toShortString() {
        return "#" + id + ": " + subject;
    }

    @Override
    public String toString() {
        return project + "\n" + toShortString();
    }
}
