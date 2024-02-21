package com.godaddy.vps4.shopper.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
