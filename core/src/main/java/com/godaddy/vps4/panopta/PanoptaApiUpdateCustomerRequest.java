package com.godaddy.vps4.panopta;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * {
 * "email_address": null,
 * "name": null,
 * "package": null,
 * "status": "active"
 * }
 */

public class PanoptaApiUpdateCustomerRequest {
    @JsonProperty("email_address")
    private String emailAddress;
    @JsonProperty("name")
    private String name;
    @JsonProperty("package")
    private String panoptaPackage;
    @JsonProperty("status")
    private String status;

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() { return emailAddress; }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void setPanoptaPackage(String panoptaPackage) {
        this.panoptaPackage = panoptaPackage;
    }

    public String getPanoptaPackage() { return panoptaPackage; }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
