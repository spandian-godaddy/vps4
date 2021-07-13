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
public class SendFailoverCompletedEmail extends SendMessagingEmailBase implements Command<FailOverEmailRequest, SendMessagingEmailBase.Response> {

    private static final Logger logger = LoggerFactory.getLogger(SendFailoverCompletedEmail.class);

    final Vps4MessagingService messagingService;

    @Inject
    public SendFailoverCompletedEmail(Vps4MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public Response execute(CommandContext context, FailOverEmailRequest emailRequest) {
        logger.info("Sending SystemDownFailoverEmail for shopper {}", emailRequest.shopperId);
        String messageId = context.execute("SendFailoverCompletedEmail-" + emailRequest.shopperId,
                ctx -> messagingService.sendFailoverCompletedEmail(emailRequest.shopperId, emailRequest.accountName, emailRequest.isManaged),
                String.class);
        this.waitForMessageComplete(context, messageId, emailRequest.shopperId);
        Response returnResponse = new Response();
        returnResponse.messageId = messageId;
        return returnResponse;
    }
}
