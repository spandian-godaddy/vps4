package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.godaddy.vps4.vm.VmOutage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.orchestration.messaging.SendMessagingEmailBase;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "SendVmOutageEmail",
        requestType = VmOutageEmailRequest.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class SendVmOutageEmail extends SendMessagingEmailBase implements Command<VmOutageEmailRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SendVmOutageEmail.class);
    private final Vps4MessagingService messagingService;
    private final VmAlertService vmAlertService;
    private static String SSL_EXPIRING_WARNING = "SSL certificate is expiring";

    @Inject
    public SendVmOutageEmail(Vps4MessagingService vps4MessagingService, VmAlertService vmAlertService) {
        this.messagingService = vps4MessagingService;
        this.vmAlertService = vmAlertService;
    }

    @Override
    public Void execute(CommandContext context, VmOutageEmailRequest req) {
        for (VmMetric metric : req.vmOutage.metrics) {
            executeForMetric(context, req, metric);
        }
        for (VmOutage.DomainMonitoringMetadata domainMetric : req.vmOutage.domainMonitoringMetadata) {
            executeForHttpAndHttps(context, req, domainMetric);
        }
        return null;
    }

    private void executeForHttpAndHttps(CommandContext context, VmOutageEmailRequest req, VmOutage.DomainMonitoringMetadata metricMetadata) {
        String metricDomain = metricMetadata.metric + " (" + metricMetadata.additionalFqdn + ")";
        String messageId;
        if (emailAlertForMetricIsEnabled(req.vmId, metricMetadata.metric.toString())) {
            if (metricMetadata.metadata.equals(SSL_EXPIRING_WARNING)) {
                logger.warn("SSL Expiring Warning detected - no outage emails sent for shopper id {}, vm id {}.", req.shopperId,
                req.vmId);
                return;
            }

            messageId = context.execute("SendVmOutageEmail-" + metricMetadata.metric,
                        ctx -> messagingService
                                .sendServicesDownEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                        metricDomain, req.vmOutage.started, req.managed),
                        String.class);

            if (messageId != null) {
                context.execute("WaitForMessageComplete-" + metricMetadata.metric, ctx -> {
                    waitForMessageComplete(ctx, messageId, req.shopperId);
                    return null;
                }, Void.class);
            } else {
                logger.warn("No outage email sent, message id was null for shopper id {}, vm id {}.", req.shopperId,
                        req.vmId);
            }
        } else {
            logger.info(
                    "No emails will be sent since email alert for metric {} is disabled for shopper id {}, vm id {}.",
                    req.vmOutage.toString(), req.shopperId, req.vmId);
        }
    }

    private void executeForMetric(CommandContext context, VmOutageEmailRequest req, VmMetric metric) {
        logger.info("Sending outage email for shopper {} and vm {}", req.shopperId, req.vmId);
        String messageId;
        if (emailAlertForMetricIsEnabled(req.vmId, metric.toString())) {
            switch (metric) {
                case PING:
                    messageId = context.execute("SendVmOutageEmail-" + metric,
                            ctx -> messagingService
                                    .sendUptimeOutageEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                            req.vmOutage.started, req.managed),
                            String.class);
                    break;

                case CPU:
                case RAM:
                case DISK:
                    Pattern pattern = Pattern.compile("^.* (\\d+%) .*$");
                    Matcher matcher = pattern.matcher(req.vmOutage.reason);
                    String percent = (matcher.find()) ? matcher.group(1) : "95%";

                    messageId = context.execute("SendVmOutageEmail-" + metric,
                            ctx -> messagingService
                                    .sendServerUsageOutageEmail(req.shopperId, req.accountName, req.ipAddress,
                                                                req.orionGuid, metric.name(), percent,
                                                                req.vmOutage.started, req.managed),
                            String.class);
                    break;

                case FTP:
                case SSH:
                case SMTP:
                case IMAP:
                case POP3:
                    messageId = context.execute("SendVmOutageEmail-" + metric,
                            ctx -> messagingService
                                    .sendServicesDownEmail(req.shopperId, req.accountName, req.ipAddress, req.orionGuid,
                                            metric.name(), req.vmOutage.started, req.managed),
                            String.class);
                    break;

                case HTTP:
                case HTTPS:
                    return;
                case UNKNOWN:
                default:
                    logger.warn("Could not determine metric type, no outage email sent for shopper id {}, vm id {}.",
                            req.shopperId, req.vmId);
                    return;
            }
            if (messageId != null) {
                context.execute("WaitForMessageComplete-" + metric, ctx -> {
                    waitForMessageComplete(ctx, messageId, req.shopperId);
                    return null;
                }, Void.class);
            } else {
                logger.warn("No outage email sent, message id was null for shopper id {}, vm id {}.", req.shopperId,
                        req.vmId);
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
