package com.godaddy.vps4.messaging.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    public String messageId;
    public String status;
    public String createdAt;
    public String templateNamespaceKey;
    public String templateTypeKey;
    public Integer privateLabelId;
    public String shopperId;
    public String failureReason;
    public List<MessagingEmail> emails = null;

    // Error response
    public String message;
    public String code;
    public List<String> stack;
    public List<MessagingErrorFields> fields;

    @Override
    public String toString() {
        if (StringUtils.isEmpty(code)) {
            return "Message [messageId: " + messageId + " status: " + status +
                    " createdAt: " + createdAt + " templateNamespaceKey: " + templateNamespaceKey +
                    " templateTypeKey: " + templateTypeKey + " privateLabelId: " + privateLabelId +
                    " shopperId: " + shopperId + " failureReason: " + failureReason + "]";
        }
        else {
            return "Error message response [code: " + code + " message: " + message + "]";
        }
    }

    public enum Statuses {
        PURGED,
        PENDING,
        FAILED,
        SUCCESS
    }
}
