package com.godaddy.vps4.orchestration.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;


public class WaitForMessageComplete implements Command<String, Void> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForMessageComplete.class);

    final Vps4MessagingService messagingService;

    @Inject
    public WaitForMessageComplete(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Void execute(CommandContext context, String messageId) {
        Message message = this.getMessageById(messageId);

        if (message.status != null) {
            while (message.status.equalsIgnoreCase(Message.Statuses.PENDING.toString())) {
                logger.debug("waiting on message id: {}", messageId);
                context.sleep(3000);
                message = this.getMessageById(messageId);
            }

            if (message.status.equalsIgnoreCase(Message.Statuses.SUCCESS.toString())) {
                logger.info("Message.toString(): {} ", message.toString());
                logger.info("Message id {} sent successfully for shopper [{}].", messageId, message.shopperId);
            } else if (message.status.equalsIgnoreCase(Message.Statuses.FAILED.toString())) {
                String errorMessage = String.format("Message %s failed: %s", message.messageId, message.failureReason);
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            } else {
                logger.error("Message {} status: {}", message.messageId, message.status);
            }
        } else {
            logger.error("GET response for Message ID {} did not return with success status ", messageId);
            logger.error("Message response: {}", message.toString());
        }

        return null;
    }

    private Message getMessageById(String messageId) {
        try {
            return messagingService.getMessageById(messageId);
        } catch (Exception e) {
            String exceptionMessage = String.format("Exception calling messagingService.getMessageById: %s",
                    e.getMessage());
            throw new RuntimeException(exceptionMessage, e);
        }
    }
}
