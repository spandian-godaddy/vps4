package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagingEmail {

    public Integer emailId;
    public Integer templateId;
    public String to;
    public String status;
    public String createdAt;
    public String currency;
    public String marketId;
    public String emailFormat;
    public String failureReason;

    @Override
    public String toString() {
        return "MessagingEmail [emailId: " + emailId + " templateId: " + templateId +
                " to: " + to + " status: " + status + " createdAt: " + createdAt +
                " currency: " + currency + " marketId: " + marketId +
                " emailFormat: " + emailFormat + " failureReason: " + failureReason +"]";
    }
}
