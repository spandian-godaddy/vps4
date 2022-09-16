package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


public class JsdApiSearchIssueRequest {
    public String jql;
    public int maxResults;
    public int startAt;
    public String[] fields;

    public JsdApiSearchIssueRequest(String jql, int maxResults, int startAt, String[] fields) {
        this.jql = jql;
        this.maxResults = maxResults;
        this.startAt = startAt;
        this.fields = fields;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}