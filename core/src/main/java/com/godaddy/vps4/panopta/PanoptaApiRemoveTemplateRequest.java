package com.godaddy.vps4.panopta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PanoptaApiRemoveTemplateRequest {
    @JsonProperty("strategy")
    public String strategy;

    public PanoptaApiRemoveTemplateRequest(String strategy){
        this.strategy = strategy;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
