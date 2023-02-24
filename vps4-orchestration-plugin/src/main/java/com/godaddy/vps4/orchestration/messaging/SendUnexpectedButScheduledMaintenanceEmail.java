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
    name = "SendUnexpectedButScheduledMaintenanceEmail",
    requestType = ScheduledMaintenanceEmailRequest.class,
    retryStrategy = CommandRetryStrategy.NEVER
)
public class SendUnexpectedButScheduledMaintenanceEmail
        implements Command<ScheduledMaintenanceEmailRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger(SendUnexpectedButScheduledMaintenanceEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendUnexpectedButScheduledMaintenanceEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public String execute(CommandContext context, ScheduledMaintenanceEmailRequest emailRequest) {
        String messageId = context.execute("SendUnscheduledMaintEmail-" + emailRequest.shopperId,
                ctx -> messagingService.sendUnexpectedButScheduledMaintenanceEmail(
                        emailRequest.shopperId, emailRequest.accountName, emailRequest.startTime,
                        emailRequest.durationMinutes, emailRequest.isManaged),
                String.class);
        logger.info("Sending UnexpectedButScheduledMaintenanceEmail with message ID {} for shopper {}", messageId, emailRequest.shopperId);
        return messageId;
    }
}
