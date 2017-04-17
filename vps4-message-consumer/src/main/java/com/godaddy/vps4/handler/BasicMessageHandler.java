package com.godaddy.vps4.handler;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicMessageHandler implements Vps4MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(BasicMessageHandler.class);

    private AtomicInteger messageCount = new AtomicInteger(0);
    
    @Override
    public void handleMessage(String message) throws Vps4MessageHandlerException {
        logger.info("Consumed message: {} ", message);
        this.messageCount.incrementAndGet();
    }

    public int getMessageCount() {
        return messageCount.get();
    }

}
