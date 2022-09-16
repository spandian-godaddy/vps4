package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JsdApiIssueCommentRequest {
    @JsonProperty("body")
    public JsdContentDoc body;

    public JsdApiIssueCommentRequest(JsdContentDoc body) { this.body = body; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}