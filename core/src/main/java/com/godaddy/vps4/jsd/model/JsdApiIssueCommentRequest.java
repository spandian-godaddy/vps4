package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class JsdApiIssueCommentRequest {
    @JsonProperty("body")
    public JsdContentDoc body;
    @JsonProperty("properties")
    public List<JsdCommentProperty> commentProperties;
    public JsdApiIssueCommentRequest(JsdContentDoc body) { this.body = body; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static class JsdCommentProperty {
        public String key;
        public PropertyValue value;
        public JsdCommentProperty(String key, PropertyValue value) { this.key = key; this.value = value; }

    }

    public static class PropertyValue {
        public boolean internal;
        public PropertyValue(boolean internal) { this.internal = internal; }

    }
}