package com.godaddy.vps4.messaging.models;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    // Error response
    public String message;
    public String code;
    public List<String> stack;
    public List<MessagingErrorFields> fields;

    @Override
    public String toString() {
        if (StringUtils.isEmpty(code)) {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
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
