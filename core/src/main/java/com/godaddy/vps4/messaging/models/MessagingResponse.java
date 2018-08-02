package com.godaddy.vps4.messaging.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagingResponse {

    public String messageId;

    // Error response
    public String message;
    public String code;
    public List<String> stack;
    public List<MessagingErrorFields> fields;

    @Override
    public String toString() {
        if (StringUtils.isEmpty(code)) {
            return "MessagingMessageId [messageId: " + messageId + "]";
        }
        else {
            return "Error message response [code: " + code + " message: " + message + "]";
        }
    }
}