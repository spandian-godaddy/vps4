package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import java.util.Arrays;
import java.util.List;

@CommandMetadata(
        name = "SendSetupCompletedEmail",
        requestType = SetupCompletedEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendSetupCompletedEmail extends SendMessagingEmailBase implements Command<SetupCompletedEmailRequest, String> {

    private static final Logger logger = LoggerFactory.getLogger(SendSetupCompletedEmail.class);

    final Vps4MessagingService messagingService;
    final CreditService creditService;
    final Config config;
    final List<String> resellerBlacklist;

    @Inject
    public SendSetupCompletedEmail(Vps4MessagingService messagingService, CreditService creditService, Config config) {
        this.messagingService = messagingService;
        this.creditService = creditService;
        this.config = config;
        resellerBlacklist = Arrays.asList(this.config.get("messaging.reseller.blacklist.setup", "").split(","));
    }

    @Override
    public String execute(CommandContext context, SetupCompletedEmailRequest emailRequest) {
        logger.info("Sending SetupCompleted for customerId {}", emailRequest.customerId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(emailRequest.orionGuid);
        String resellerId = credit.getResellerId();

        if (resellerBlacklist.contains(resellerId)){
            throw new RuntimeException(String.format("Credit's Reseller Id %s is suppressed for email template VirtualPrivateHostingProvisioned4. " +
                    "No longer attempting to send SetupCompleted to shopper %s", resellerId, emailRequest.customerId));
        }
        String messageId = context.execute("SendSetupCompletedEmail-" + emailRequest.customerId,
                ctx -> messagingService.sendSetupEmail(emailRequest.customerId, emailRequest.serverName, emailRequest.ipAddress,
                        emailRequest.orionGuid.toString(), emailRequest.isManaged),
                String.class);
        this.waitForMessageComplete(context, messageId, emailRequest.customerId);
        return messageId;
    }
}
