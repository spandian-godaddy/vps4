package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShopperOverride {

    public String email;
    public Contact contact;
    public Preference preference;

    @Override
    public String toString() {
        return "ShopperOverride [email: " + email + " contact: " + contact +
                " preference: " + preference + "]";
    }
}
