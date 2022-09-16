package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collections;
import java.util.List;

public class JsdContentNodeLabel extends JsdContentNodeValue {
    private static final String CONTENT_TYPE_TEXT = "text";
    private static final String CONTENT_MARKS = "strong";

        @JsonProperty("marks") public List<Object> marks;
        public JsdContentNodeLabel(String text){
            super(text);
            this.marks = Collections.singletonList(new JsdContentNodeMarks(CONTENT_MARKS));
            this.type = CONTENT_TYPE_TEXT;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }