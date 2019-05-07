package com.godaddy.vps4.handler;

import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;

public class Vps4AccountMessage extends JsonMessage {

    public UUID id;
    public UUID accountGuid;
    MessageNotificationType notificationType;

    public Vps4AccountMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        super(message);
        id = UUID.fromString(value.get("id").toString());
        accountGuid = UUID.fromString(((JSONObject) value.get("notification")).get("account_guid").toString());
        String typePreFormat = ((JSONObject) value.get("notification")).get("type").toString();
        notificationType = MessageNotificationType.valueOf(typePreFormat.toUpperCase());
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
