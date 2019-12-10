package com.godaddy.vps4.handler;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class Vps4PanoptaMessage extends JsonMessage {

    public String event;
    public String serverKey;
    public String itemType;
    public String start;
    public String reasons;
    public String outageId;

    public Vps4PanoptaMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        super(message);
        event = value.get("event").toString();
        serverKey = value.get("serverKey").toString();
        itemType = value.get("itemType").toString();
        start = value.get("start").toString();
        reasons = value.get("reasons").toString();
        outageId = value.get("outageId").toString();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
