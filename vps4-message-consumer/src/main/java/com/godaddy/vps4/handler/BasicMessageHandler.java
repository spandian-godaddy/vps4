package com.godaddy.vps4.handler;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(BasicMessageHandler.class);

    private AtomicInteger messageCount = new AtomicInteger(0);

    @Override
    public void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        logger.info("Consumed message: {} ", message.value());
        this.messageCount.incrementAndGet();
    }

    public int getMessageCount() {
        return messageCount.get();
    }

}
