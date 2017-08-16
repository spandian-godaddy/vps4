package com.godaddy.vps4.messaging.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

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

    @Override
    public String toString() {
        return "Message [messageId: " + messageId + " status: " + status +
                " createdAt: " + createdAt + " templateNamespaceKey: " + templateNamespaceKey +
                " templateTypeKey: " + templateTypeKey + " privateLabelId: " + privateLabelId +
                " shopperId: " + shopperId + " failureReason: " + failureReason + "]";
    }
}
