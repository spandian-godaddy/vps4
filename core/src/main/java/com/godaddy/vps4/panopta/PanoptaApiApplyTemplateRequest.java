package com.godaddy.vps4.panopta;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {
 *   "continuous": true,
 *   "serverTemplate": "https://api2.panopta.com/v2/server_template/1146060"
 * }
 */
public class PanoptaApiApplyTemplateRequest {

    @JsonProperty("continuous")
    private boolean continuous;
    @JsonProperty("server_template")
    private String serverTemplate;

    public PanoptaApiApplyTemplateRequest(String serverTemplate, boolean continuous)
    {
        this.serverTemplate = serverTemplate;
        this.continuous = continuous;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public String getServerTemplate() {
        return serverTemplate;
    }

    public void setServerTemplate(String serverTemplate) {
        this.serverTemplate = serverTemplate;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
