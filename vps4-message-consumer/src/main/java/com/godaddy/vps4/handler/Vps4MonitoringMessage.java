package com.godaddy.vps4.handler;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class Vps4MonitoringMessage extends JsonMessage {

    public String event;
    public long hfsCheckId;

    public Vps4MonitoringMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        super(message);
        this.event = this.value.get("event").toString();
        this.hfsCheckId = Long.valueOf(this.key);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
