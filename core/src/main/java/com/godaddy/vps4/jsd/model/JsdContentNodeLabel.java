package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collections;
import java.util.List;

public class JsdContentNodeLabel extends JsdContentNodeValue {
    private static final String CONTENT_TYPE_TEXT = "text";
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("marks") public List<Object> marks;
    public JsdContentNodeLabel(String text, JsdContentNodeMarks marks){
        super(text);
        this.marks = marks == null ? null : Collections.singletonList(marks);
        this.type = CONTENT_TYPE_TEXT;
    }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }