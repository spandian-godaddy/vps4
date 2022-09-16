package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class JsdContentParagraph {
    private static final String CONTENT_TYPE_PARAGRAPH = "paragraph";

    @JsonProperty("content") public List<Object> contentList;
    @JsonProperty("type") public String type;

    public JsdContentParagraph(List<Object> contentList) {
        this.type = CONTENT_TYPE_PARAGRAPH;
        this.contentList = contentList;
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
