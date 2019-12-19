package com.godaddy.vps4.orchestration.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
    name = "SendScheduledPatchingEmail",
    requestType = ScheduledMaintenanceEmailRequest.class,
    retryStrategy = CommandRetryStrategy.NEVER
)
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
        logger.info("Sending ScheduledPatchingEmail for shopper {}", emailRequest.shopperId);
        String messageId = context.execute("SendPatchingEmail-" + emailRequest.shopperId,
                ctx -> messagingService.sendScheduledPatchingEmail(emailRequest.shopperId,
                        emailRequest.accountName, emailRequest.startTime,
                        emailRequest.durationMinutes, emailRequest.isManaged),
                String.class);
        this.waitForMessageComplete(context, messageId, emailRequest.shopperId);

        return null;
    }
}
