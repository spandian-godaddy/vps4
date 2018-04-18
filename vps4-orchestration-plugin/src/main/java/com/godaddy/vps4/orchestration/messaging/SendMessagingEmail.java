package com.godaddy.vps4.orchestration.messaging;

import java.io.IOException;
import com.godaddy.vps4.messaging.MissingShopperIdException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import com.godaddy.vps4.messaging.Vps4MessagingService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;


public class SendMessagingEmail implements Command<EmailRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SendMessagingEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendMessagingEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Void execute(CommandContext context, EmailRequest emailRequest) {
        String messageId = "";

        try {
            logger.info("Sending email {} for shopper {}", emailRequest.template, emailRequest.shopperId);
            messageId = emailRequest.sendEmail(messagingService);
            this.waitForMessageComplete(context, messageId, emailRequest.shopperId);
        } catch (IOException | MissingShopperIdException e) {
            String message = String.format("Exception during sending %s for shopper %s",
                    emailRequest.template, emailRequest.shopperId);
            throw new RuntimeException(message, e);
        }

        return null;
    }

    private void waitForMessageComplete(CommandContext context, String messageId, String shopperId) {
        if (messageId.isEmpty()) {
            String exceptionMessage = String.format("Error sending email, messageId is empty for shopper %s",
                    shopperId);
            throw new RuntimeException(exceptionMessage);
        }

        context.execute(WaitForMessageComplete.class, messageId);
    }
}
