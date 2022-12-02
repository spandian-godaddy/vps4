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
    name = "SendFailoverCompletedEmail",
    requestType = FailOverEmailRequest.class,
    retryStrategy = CommandRetryStrategy.NEVER
)
public class SendFailoverCompletedEmail implements Command<FailOverEmailRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger(SendFailoverCompletedEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendFailoverCompletedEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public String execute(CommandContext context, FailOverEmailRequest emailRequest) {
        String messageId = context.execute("SendFailoverCompletedEmail-" + emailRequest.shopperId,
                ctx -> messagingService.sendFailoverCompletedEmail(emailRequest.shopperId, emailRequest.accountName, emailRequest.isManaged),
                String.class);
        logger.info("Sending SystemDownFailoverEmail with message ID {} for shopper {}",messageId, emailRequest.shopperId);
        return messageId;
    }
}
