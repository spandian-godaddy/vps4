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
        name = "SendSetupCompletedEmail",
        requestType = SetupCompletedEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendSetupCompletedEmail extends SendMessagingEmailBase implements Command<SetupCompletedEmailRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger(SendSetupCompletedEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendSetupCompletedEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public String execute(CommandContext context, SetupCompletedEmailRequest emailRequest) {
        logger.info("Sending SetupCompleted for shopper {}", emailRequest.shopperId);
        String messageId = context.execute("SendSetupCompletedEmail-" + emailRequest.shopperId,
                ctx -> messagingService.sendSetupEmail(emailRequest.shopperId, emailRequest.serverName, emailRequest.ipAddress,
                        emailRequest.orionGuid.toString(), emailRequest.isManaged),
                String.class);
        this.waitForMessageComplete(context, messageId, emailRequest.shopperId);
        return messageId;
    }
}
