package com.hiberus.anaya.redmineapi;

import org.json.JSONObject;

public final class Issue {
    public final int id;
    public final String description;
    public final String project;

    public Issue(JSONObject rawIssue) {
        this.id = rawIssue.getInt("id");
        this.description = rawIssue.optString("subject", "");
        this.project = rawIssue.getJSONObject("project").optString("name", "");
    }

    @Override
    public String toString() {
        return project + "\n#" + id + ": " + description;
    }
}
