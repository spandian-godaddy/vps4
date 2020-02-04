package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagingEmail {

    public Long emailId;
    public Integer templateId;
    public String templateName;
    public String to;
    public String status;
    public String createdAt;
    public String currency;
    public String marketId;
    public String failureReason;

    @Override
    public String toString() {
        return "MessagingEmail [emailId=" + emailId + ", templateId=" + templateId + ", templateName=" + templateName
                + ", to=" + to + ", status=" + status + ", createdAt=" + createdAt + ", currency=" + currency
                + ", marketId=" + marketId + ", failureReason=" + failureReason + "]";
    }
}
