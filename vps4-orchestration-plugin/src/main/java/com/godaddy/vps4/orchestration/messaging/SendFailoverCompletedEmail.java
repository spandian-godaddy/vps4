package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class SendFailoverCompletedEmail extends SendMessagingEmailBase implements Command<FailOverEmailRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SendFailoverCompletedEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendFailoverCompletedEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Void execute(CommandContext context, FailOverEmailRequest emailRequest) {
        try {
            logger.info("Sending SystemDownFailoverEmail for shopper {}", emailRequest.shopperId);
            String messageId = messagingService.sendFailoverCompletedEmail(emailRequest.shopperId,
                    emailRequest.accountName, emailRequest.isFullyManaged);
            this.waitForMessageComplete(context, messageId, emailRequest.shopperId);
        } catch (IOException | MissingShopperIdException ex) {
            String exceptionMessage = String.format("Exception calling messagingService.sendFailoverCompletedEmail: %s",
                    ex.getMessage());
            throw new RuntimeException(exceptionMessage, ex);
        }

        return null;
    }
}
