package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import java.util.Arrays;
import java.util.Collections;
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
        String rer = this.config.get("messaging.reseller.blacklist.setup", "");
        String[] erwer = this.config.get("messaging.reseller.blacklist.setup", "").split(",");
        resellerBlacklist = Arrays.asList(this.config.get("messaging.reseller.blacklist.setup", "").split(","));
    }

    @Override
    public String execute(CommandContext context, SetupCompletedEmailRequest emailRequest) {
        logger.info("Sending SetupCompleted for shopper {}", emailRequest.shopperId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(emailRequest.orionGuid);
        String resellerId = credit.getResellerId();

        if (resellerBlacklist.contains(resellerId)){
            logger.info("Credit's Reseller Id {} is suppressed for email template VirtualPrivateHostingProvisioned4. " +
                    "No longer attempting to send SetupCompleted to shopper {}", resellerId, emailRequest.shopperId);
            return null;
        }
        String messageId = context.execute("SendSetupCompletedEmail-" + emailRequest.shopperId,
                ctx -> messagingService.sendSetupEmail(emailRequest.shopperId, emailRequest.serverName, emailRequest.ipAddress,
                        emailRequest.orionGuid.toString(), emailRequest.isManaged),
                String.class);
        this.waitForMessageComplete(context, messageId, emailRequest.shopperId);
        return messageId;
    }
}
