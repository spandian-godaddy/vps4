package com.godaddy.vps4.messaging.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagingMessageId {

    public String messageId;

    @Override
    public String toString() {
        return "MessagingMessageId [messageId: " + messageId + "]";
    }
}