package com.godaddy.vps4.customer;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {
    private final UUID customerId;
    private final Identity identity;

    @JsonCreator
    public Customer(@JsonProperty("customerId") UUID customerId,
                    @JsonProperty("identity") Identity identity) {
        this.customerId = customerId;
        this.identity = identity;
    }

    public static class Identity {
        @JsonAlias({"id"}) public String shopperId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getShopperId() {
        return identity.shopperId;
    }
}
