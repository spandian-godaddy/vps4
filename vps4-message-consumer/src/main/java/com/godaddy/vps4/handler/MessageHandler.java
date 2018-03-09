package com.godaddy.vps4.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface MessageHandler {

    void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException;
}

