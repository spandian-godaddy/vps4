package com.godaddy.vps4.panopta;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {
 *   "email_address": null,
 *   "name": null,
 *   "package": null,
 *   "partner_customer_key": null
 * }
 */
public class PanoptaApiCustomerRequest {

    @JsonProperty("email_address")
    private String emailAddress;
    @JsonProperty("name")
    private String name;
    @JsonProperty("package")
    private String panoptaPackage;
    @JsonProperty("partner_customer_key")
    private String partnerCustomerKey;

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPanoptaPackage(String panoptaPackage) {
        this.panoptaPackage = panoptaPackage;
    }

    public void setPartnerCustomerKey(String partnerCustomerKey) {
        this.partnerCustomerKey = partnerCustomerKey;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
