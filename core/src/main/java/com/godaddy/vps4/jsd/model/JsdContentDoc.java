package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class JsdContentDoc extends JsdContentParagraph {
    private static final String CONTENT_TYPE_DOC = "doc";
    private static final int CONTENT_VERSION = 1;

    @JsonProperty("version") public Integer version;

    public JsdContentDoc(List<Object> contentList) {
        super(contentList);
        this.type = CONTENT_TYPE_DOC;
        this.version = CONTENT_VERSION;
    }
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
