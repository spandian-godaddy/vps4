package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.vm.VmOutage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "SendVmOutageResolvedEmail",
        requestType = VmOutageEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendVmOutageResolvedEmail implements Command<VmOutageEmailRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SendVmOutageResolvedEmail.class);
    private final Vps4MessagingService messagingService;
    private final VmAlertService vmAlertService;
    private static String SSL_EXPIRING_WARNING = "SSL certificate is expiring";

    @Inject
    public SendVmOutageResolvedEmail(Vps4MessagingService vps4MessagingService, VmAlertService vmAlertService) {
        this.messagingService = vps4MessagingService;
        this.vmAlertService = vmAlertService;
    }

    @Override
    public Void execute(CommandContext context, VmOutageEmailRequest req) {
        for (VmMetric metric : req.vmOutage.metrics) {
            executeForMetric(context, req, metric);
        }
        for (VmOutage.DomainMonitoringMetadata domainMetric : req.vmOutage.domainMonitoringMetadata) {
            executeForHTTPandHTTPS(context, req, domainMetric);
        }
        return null;
    }

    private void executeForHTTPandHTTPS(CommandContext context, VmOutageEmailRequest req, VmOutage.DomainMonitoringMetadata metricMetadata) {
        String metricDomain = metricMetadata.metric + " (" + metricMetadata.additionalFqdn + ")";
        String messageId;
        if (emailAlertForMetricIsEnabled(req.vmId, metricMetadata.metric.toString())) {
            if(metricMetadata.metadata.size() == 1 && metricMetadata.metadata.get(0).equals(SSL_EXPIRING_WARNING)) {
                logger.warn("SSL Expiring Warning detected - no outage emails sent for shopper id {}, vm id {}.", req.shopperId,
                        req.vmId);
                return;
            }

            messageId = context.execute("SendVmOutageResolvedEmail-" + metricMetadata.metric,
                        ctx -> messagingService
                                .sendServiceOutageResolvedEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                        metricDomain, req.vmOutage.ended, req.managed),
                        String.class);

            if (messageId != null) {
                logger.info("Outage resolved message ID {} sent for VMID {}", messageId, req.vmId);
            } else {
                logger.warn("No outage resolved email sent, message id was null for shopper id {}.", req.shopperId);
            }
        } else {
            logger.info(
                    "No emails will be sent since email alert for metric {} is disabled for shopper id {}, vm id {}.",
                    req.vmOutage.toString(), req.shopperId, req.vmId);
        }
    }
    private void executeForMetric(CommandContext context, VmOutageEmailRequest req, VmMetric metric) {
        logger.info("Sending outage resolved email for shopper {} and vm {}", req.shopperId, req.vmId);

        if (emailAlertForMetricIsEnabled(req.vmId, metric.toString())) {
            String messageId;
            switch (metric) {
                case PING:
                    messageId = context.execute("SendVmOutageResolvedEmail-" + metric,
                            ctx -> messagingService
                                    .sendUptimeOutageResolvedEmail(req.shopperId, req.accountName, req.ipAddress,
                                            req.orionGuid, req.vmOutage.ended, req.managed),
                            String.class);
                    break;

                case CPU:
                case RAM:
                case DISK:
                    messageId = context.execute("SendVmOutageResolvedEmail-" + metric,
                            ctx -> messagingService
                                    .sendUsageOutageResolvedEmail(req.shopperId, req.accountName, req.ipAddress,
                                            req.orionGuid, metric.name(), req.vmOutage.ended, req.managed),
                            String.class);
                    break;

                case FTP:
                case SSH:
                case SMTP:
                case IMAP:
                case POP3:
                    logger.info("sending resolved email for {}", metric);
                    messageId = context.execute("SendVmOutageResolvedEmail-" + metric,
                            ctx -> messagingService
                                    .sendServiceOutageResolvedEmail(req.shopperId, req.accountName, req.ipAddress,
                                            req.orionGuid, metric.name(), req.vmOutage.ended, req.managed),
                            String.class);
                    break;

                case HTTP:
                case HTTPS:
                    return;
                case UNKNOWN:
                default:
                    logger.warn(
                            "Metric type not determined, no outage resolved email sent for shopper id {}, vm id {}.",
                            req.shopperId, req.vmId);
                    return;
            }
            if (messageId != null) {
                logger.info("Outage Resolved message ID {} sent for VMID {}", messageId, req.vmId);
            } else {
                logger.warn(
                        "No outage resolved email sent, message id was null for shopper id {}.", req.shopperId);
            }
        } else {
            logger.info(
                    "No emails will be sent since email alert for metric {} is disabled for shopper id {}, vm id {}.",
                    req.vmOutage.toString(), req.shopperId, req.vmId);
        }
    }

    private boolean emailAlertForMetricIsEnabled(UUID vmId, String metric) {
        VmMetricAlert alert = vmAlertService.getVmMetricAlert(vmId, metric);
        return (alert.status == VmMetricAlert.Status.ENABLED);
    }
}
