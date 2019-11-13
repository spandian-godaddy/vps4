package com.godaddy.vps4.handler;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.CommandService;

public class Vps4PanoptaMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4PanoptaMessageHandler.class);

    private final CommandService commandService;

    @Inject
    public Vps4PanoptaMessageHandler(CommandService commandService) {
        this.commandService = commandService;
    }


    @Override
    public void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        logger.info("Consumed message: key: {}, value: {} ", message.key(), message.value());
        JsonMessage jsonMsg = new JsonMessage(message);
    }

}
