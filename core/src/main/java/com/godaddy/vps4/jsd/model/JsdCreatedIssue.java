package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsdCreatedIssue {
    public Long issueId;
    public String issueKey;

    @JsonProperty("id")
    private void mapIssueId(String id) {
        this.issueId = Long.parseLong(id);
    }

    @JsonProperty("key")
    private void mapIssueKey(String key) { this.issueKey = key; }
}
