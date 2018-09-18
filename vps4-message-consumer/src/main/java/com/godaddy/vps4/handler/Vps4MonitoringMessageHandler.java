package com.godaddy.vps4.handler;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.handler.util.Commands;

import gdg.hfs.orchestration.CommandService;

import static com.godaddy.vps4.handler.util.Utils.isOrchEngineDown;
import static com.godaddy.vps4.handler.util.Utils.isDBError;

public class Vps4MonitoringMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4MonitoringMessageHandler.class);

    private final CommandService commandService;
    private final String VM_DOWN = "down";

    @Inject
    public Vps4MonitoringMessageHandler(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        logger.info("Consumed message: {} ", message.value());
        Vps4MonitoringMessage vps4Message = new Vps4MonitoringMessage(message);

        if (vps4Message.event.toLowerCase().equals(VM_DOWN)) {
            try {
                Commands.execute(commandService, "HandleMonitoringDownEvent", vps4Message.hfsCheckId);
            } catch (Exception ex) {
                logger.error("Failed while handling monitoring message for check id {}", vps4Message.hfsCheckId);
                boolean shouldRetry = isOrchEngineDown(ex) || isDBError(ex);
                throw new MessageHandlerException(shouldRetry, ex);
            }
        }

    }

}
