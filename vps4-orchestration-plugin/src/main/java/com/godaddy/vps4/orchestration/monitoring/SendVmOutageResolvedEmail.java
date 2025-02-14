package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@CommandMetadata(
        name = "SendVmOutageResolvedEmail",
        requestType = VmOutageEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendVmOutageResolvedEmail extends SendVmOutageEmail {

    private final MessagingService messagingService;

    @Inject
    public SendVmOutageResolvedEmail(MessagingService messagingService, VmAlertService vmAlertService) {
        super(vmAlertService);
        this.messagingService = messagingService;
    }

    @Override
    protected String sendGeneralOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                            UUID orionGuid, String metricDomain, Instant alertStart, boolean isManaged, String metric) {
        String messageId = context.execute("SendVmOutageResolvedEmail-" + metric,
                ctx -> messagingService
                        .sendServiceOutageResolvedEmail(shopperId, accountName, ipAddress, orionGuid,
                                metricDomain, alertStart, isManaged),
                String.class);
        return messageId;
    }

    @Override
    protected void sendUptimeOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                         UUID orionGuid, Instant alertStart, boolean isManaged, String metric) {
        context.execute("SendVmOutageResolvedEmail-" + metric,
                ctx -> messagingService
                        .sendUptimeOutageResolvedEmail(shopperId, accountName, ipAddress, orionGuid,
                                alertStart, isManaged),
                String.class);
    }

    @Override
    protected void sendServerUsageOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                              UUID orionGuid, Instant alertStart, String reason, boolean isManaged, String metric) {
        context.execute("SendVmOutageResolvedEmail-" + metric,
                ctx -> messagingService
                        .sendUsageOutageResolvedEmail(shopperId, accountName, ipAddress,
                                orionGuid, metric,
                                alertStart, isManaged),
                String.class);
    }
}
