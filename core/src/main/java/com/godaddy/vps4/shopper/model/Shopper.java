package com.godaddy.vps4.shopper.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
Example response
 * {
    "shopperId": "912348",
    "createdAt": "2013-08-21T02:27:06.000Z",
    "updatedAt": "2024-02-29T22:46:02.000Z",
    "email": "employee@godaddy.com",
    "customerId": "9f77f045-bde2-4d4f-b8d1-ab64a99b1b53",
    "privateLabelId": 1,
    "hasCredentials": true
}
 */

public class Shopper {
    private final UUID customerId;
    private final String shopperId;
    private final String parentShopperId;
    private final String privateLabelId;

    @JsonCreator
    public Shopper(@JsonProperty("customerId") UUID customerId,
                   @JsonProperty("shopperId") String shopperId,
                   @JsonProperty("parentShopperId") String parentShopperId,
                   @JsonProperty("privateLabelId") String privateLabelId) {
        this.customerId = customerId;
        this.shopperId = shopperId;
        this.parentShopperId = parentShopperId;
        this.privateLabelId = privateLabelId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getShopperId() {
        return shopperId;
    }
    public String getParentShopperId() {
        return parentShopperId;
    }
    public String getPrivateLabelId() {
        return privateLabelId;
    }
}
