package com.godaddy.vps4.orchestration.monitoring;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmOutage;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "SendVmOutageEmail",
        requestType = VmOutageEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendVmOutageEmail implements Command<VmOutageEmailRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SendVmOutageEmail.class);
    private final MessagingService messagingService;
    private final VmAlertService vmAlertService;
    private static final String SSL_EXPIRING_WARNING = "SSL certificate is expiring";

    @Inject
    public SendVmOutageEmail(MessagingService messagingService, VmAlertService vmAlertService) {
        this.messagingService = messagingService;
        this.vmAlertService = vmAlertService;
    }

    @Override
    public Void execute(CommandContext context, VmOutageEmailRequest req) {
        req.vmOutage.metrics.retainAll(getEnabledMetrics(req.vmId));
        executeForMetrics(context, req);
        for (VmOutage.DomainMonitoringMetadata domainMetric : req.vmOutage.domainMonitoringMetadata) {
            executeForHttpAndHttps(context, req, domainMetric);
        }
        return null;
    }

    private void executeForHttpAndHttps(CommandContext context, VmOutageEmailRequest req, VmOutage.DomainMonitoringMetadata metricMetadata) {
        if (metricMetadata.metadata.size() == 1 && metricMetadata.metadata.get(0).equals(SSL_EXPIRING_WARNING)) {
            String text = "SSL expiring warning detected - no outage emails sent for shopper ID {}, VM ID {}.";
            logger.warn(text, req.shopperId, req.vmId);
            return;
        }

        String metricDomain = metricMetadata.metric + " (" + metricMetadata.additionalFqdn + ")";
        String messageId = context.execute("SendVmOutageEmail-" + metricMetadata.metric,
                                           ctx -> messagingService
                                                   .sendServicesDownEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                                                          metricDomain, req.vmOutage.started, req.managed),
                                           String.class);

        if (messageId != null) {
            logger.info("Outage message ID {} sent for VM ID {}", messageId, req.vmId);
        } else {
            logger.warn("No outage email sent, message ID was null for shopper ID {}, VM ID {}", req.shopperId,
                        req.vmId);
        }
    }

    private void executeForMetrics(CommandContext context, VmOutageEmailRequest req) {
        logger.info("Sending outage email for shopper {} and vm {}", req.shopperId, req.vmId);
        Set<VmMetric> portCheckServices = new HashSet<>();
        for (VmMetric metric : req.vmOutage.metrics) {
            switch (metric) {
                case PING:
                    context.execute("SendVmOutageEmail-" + metric,
                                    ctx -> messagingService
                                            .sendUptimeOutageEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                                                   req.vmOutage.started, req.managed),
                                    String.class);
                    break;

                case CPU:
                case DISK:
                case RAM:
                    Pattern pattern = Pattern.compile("^.* (\\d+%) .*$");
                    Matcher matcher = pattern.matcher(req.vmOutage.reason);
                    String percent = (matcher.find()) ? matcher.group(1) : "95%";

                    context.execute("SendVmOutageEmail-" + metric,
                                    ctx -> messagingService
                                            .sendServerUsageOutageEmail(req.shopperId, req.accountName, req.ipAddress,
                                                                        req.orionGuid, metric.name(), percent,
                                                                        req.vmOutage.started, req.managed),
                                    String.class);
                    break;

                case FTP:
                case HTTP:
                case IMAP:
                case POP3:
                case SMTP:
                case SSH:
                    portCheckServices.add(metric);
                    break;

                case HTTP_DOMAIN:
                case HTTPS_DOMAIN:
                    break; // handled separately

                case UNKNOWN:
                default:
                    logger.warn("No outage email sent for shopper ID {}, VM ID {}.", req.shopperId, req.vmId);
            }
        }

        if (!portCheckServices.isEmpty()) {
            String portCheckServiceNames = portCheckServices.stream()
                                                            .map(Enum::name)
                                                            .collect(Collectors.joining(", "));
            context.execute("SendVmOutageEmail-Services",
                            ctx -> messagingService
                                    .sendServicesDownEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                                           portCheckServiceNames, req.vmOutage.started, req.managed),
                            String.class);
        }
    }

    private Set<VmMetric> getEnabledMetrics(UUID vmId) {
        return vmAlertService.getVmMetricAlertList(vmId)
                             .stream()
                             .filter(a -> a.status == VmMetricAlert.Status.ENABLED)
                             .map(a -> a.metric)
                             .collect(Collectors.toSet());
    }
}
