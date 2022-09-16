package com.godaddy.vps4.jsd.model;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsdContentNodeMarks {
        @JsonProperty("type") public String type;

        public JsdContentNodeMarks(String type){
            this.type = type;
        }
        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }