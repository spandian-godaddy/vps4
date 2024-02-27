package com.godaddy.vps4.orchestration.monitoring;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
    private final VmAlertService vmAlertService;
    private static final String SSL_EXPIRING_WARNING = "SSL certificate is expiring";

    @Inject
    public SendVmOutageEmail(VmAlertService vmAlertService) {
        this.vmAlertService = vmAlertService;
    }

    @Override
    public Void execute(CommandContext context, VmOutageEmailRequest req) {
        Set<VmMetric> enabledMetrics = getEnabledMetrics(req.vmId);
        req.vmOutage.metrics.retainAll(enabledMetrics);
        List<VmOutage.DomainMonitoringMetadata> filteredDomainMonitoringMetadata = req.vmOutage.domainMonitoringMetadata
                        .stream()
                        .filter(d -> enabledMetrics.contains(d.metric))
                        .collect(Collectors.toList());
        executeForMetrics(context, req);
        for (VmOutage.DomainMonitoringMetadata domainMetric : filteredDomainMonitoringMetadata) {
            executeForHttpAndHttps(context, req, domainMetric);
        }
        return null;
    }

    protected String sendGeneralOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                          UUID orionGuid, String metricDomain, Instant alertStart, boolean isManaged, String metric) {
        return null;
        // do nothing
    }

    protected void sendUptimeOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                         UUID orionGuid, Instant alertStart, boolean isManaged, String metric) {
        // do nothing
    }


    protected void sendServerUsageOutageEmail(CommandContext context, String shopperId, String accountName, String ipAddress,
                                            UUID orionGuid, Instant alertStart, String reason, boolean isManaged, String metric) {
        // do nothing
    }

    private void executeForHttpAndHttps(CommandContext context, VmOutageEmailRequest req, VmOutage.DomainMonitoringMetadata metricMetadata) {
        if (metricMetadata.metadata.size() == 1 && metricMetadata.metadata.get(0).equals(SSL_EXPIRING_WARNING)) {
            String text = "SSL expiring warning detected - no outage emails sent for shopper ID {}, VM ID {}.";
            logger.warn(text, req.shopperId, req.vmId);
            return;
        }

        String metricDomain = metricMetadata.metric + " (" + metricMetadata.additionalFqdn + ")";
        String messageId = sendGeneralOutageEmail(context, req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                metricDomain, req.vmOutage.started, req.managed, metricMetadata.metric.toString());

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
                    sendUptimeOutageEmail(context, req.shopperId, req.accountName, req.ipAddress, req.orionGuid, req.vmOutage.started, req.managed, metric.toString());
                    break;

                case CPU:
                case DISK:
                case RAM:
                    sendServerUsageOutageEmail(context, req.shopperId, req.accountName, req.ipAddress, req.orionGuid, req.vmOutage.started, req.vmOutage.reason, req.managed, metric.toString());
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
            sendGeneralOutageEmail(context, req.shopperId, req.accountName, req.ipAddress, req.orionGuid, portCheckServiceNames, req.vmOutage.started, req.managed, "Services");
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
