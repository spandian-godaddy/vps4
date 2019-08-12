package com.godaddy.vps4.panopta;

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
    public String emailAddress;
    public String name;
    @JsonProperty("package")
    public String panoptaPackage;
    @JsonProperty("partner_customer_key")
    public String partnerCustomerKey;
}
