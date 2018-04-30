package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class SendScheduledPatchingEmail extends SendMessagingEmailBase
        implements Command<ScheduledMaintenanceEmailRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SendScheduledPatchingEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendScheduledPatchingEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Void execute(CommandContext context, ScheduledMaintenanceEmailRequest emailRequest) {
        try {
            logger.info("Sending ScheduledPatchingEmail for shopper {}", emailRequest.shopperId);
            String messageId = messagingService.sendScheduledPatchingEmail(emailRequest.shopperId,
                    emailRequest.accountName, emailRequest.startTime, emailRequest.durationMinutes,
                    emailRequest.isFullyManaged);
            this.waitForMessageComplete(context, messageId, emailRequest.shopperId);
        } catch (IOException | MissingShopperIdException ex) {
            String exceptionMessage = String.format("Exception calling messagingService.sendScheduledPatchingEmail: %s",
                    ex.getMessage());
            throw new RuntimeException(exceptionMessage, ex);
        }

        return null;
    }
}
