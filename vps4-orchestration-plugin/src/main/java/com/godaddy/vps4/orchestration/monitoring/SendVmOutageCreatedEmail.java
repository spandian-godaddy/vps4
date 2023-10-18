package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandMetadata(
        name = "SendVmOutageCreatedEmail",
        requestType = VmOutageEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendVmOutageCreatedEmail extends SendVmOutageEmail {

    private final MessagingService messagingService;

    @Inject
    public SendVmOutageCreatedEmail(MessagingService messagingService, VmAlertService vmAlertService) {
        super(vmAlertService);
        this.messagingService = messagingService;
    }

    @Override
    protected String sendGeneralOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                          UUID orionGuid, String metricDomain, Instant alertStart, boolean isManaged, String metric) {
        String messageId = context.execute("SendVmOutageCreatedEmail-" + metric,
                ctx -> messagingService
                        .sendServicesDownEmail(shopperId, accountName, ipAddress, orionGuid,
                                metricDomain, alertStart, isManaged),
                String.class);
        return messageId;
    }

    @Override
    protected void sendUptimeOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                         UUID orionGuid, Instant alertStart, boolean isManaged, String metric) {
        context.execute("SendVmOutageCreatedEmail-" + metric,
                ctx -> messagingService
                        .sendUptimeOutageEmail(shopperId, accountName, ipAddress, orionGuid,
                                alertStart, isManaged),
                String.class);
    }


    @Override
    protected void sendServerUsageOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                            UUID orionGuid, Instant alertStart, String reason, boolean isManaged, String metric) {
        Pattern pattern = Pattern.compile("^.* (\\d+%) .*$");
        Matcher matcher = pattern.matcher(reason);
        String percent = (matcher.find()) ? matcher.group(1) : "95%";
        context.execute("SendVmOutageCreatedEmail-" + metric,
                ctx -> messagingService
                        .sendServerUsageOutageEmail(shopperId, accountName, ipAddress,
                                orionGuid, metric, percent,
                                alertStart, isManaged),
                String.class);
    }
}
