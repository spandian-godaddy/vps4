package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailRecipient {

    public String email;
    public Contact contact;
    public Preference preference;

    @Override
    public String toString() {
        return "EmailRecipient [email: " + email + " contact: " + contact.toString() +
                " preference: " + preference.toString() + "]";
    }
}