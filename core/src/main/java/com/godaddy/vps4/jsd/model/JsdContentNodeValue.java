package com.godaddy.vps4.jsd.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class JsdContentNodeValue extends JsdContentNodeMarks {
    private static final String CONTENT_TYPE_TEXT = "text";
        @JsonProperty("text") public String text;

        public JsdContentNodeValue(String text){
            super(CONTENT_TYPE_TEXT);
            this.text = text;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }