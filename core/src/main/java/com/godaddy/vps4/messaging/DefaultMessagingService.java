package com.godaddy.vps4.messaging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.messaging.models.Substitution;
import com.godaddy.vps4.messaging.models.TemplateType;
import com.godaddy.vps4.messaging.models.Transformation;

public class DefaultMessagingService implements MessagingService {
    private final DateTimeFormatter dateTimeFormatter;
    private final MessagingApiService messagingApiService;

    @Inject
    public DefaultMessagingService(MessagingApiService messagingApiService) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.messagingApiService = messagingApiService;
    }

    @Override
    public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionGuid,
                                 boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VirtualPrivateHostingProvisioned4)
                .substitute(Substitution.ACCOUNTNAME, accountName)
                .substitute(Substitution.IPADDRESS, ipAddress)
                .substitute(Substitution.ORION_ID, orionGuid)
                .substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendFullyManagedEmail(String shopperId, String controlPanel) {
        ShopperMessage message;
        switch (controlPanel.trim().toLowerCase()) {
            case "cpanel":
                message = new ShopperMessage(TemplateType.VPSWelcomeCpanel);
                break;
            case "plesk":
                message = new ShopperMessage(TemplateType.VPSWelcomePlesk);
                break;
            default:
                throw new IllegalArgumentException("Specified control panel not supported for fully managed email.");
        }
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    private String formatDateTime(Instant dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("GMT"));
        return dateTimeFormatter.format(zonedDateTime);
    }

    @Override
    public String sendScheduledPatchingEmail(String shopperId, String accountName, Instant startTime,
                                             long durationMinutes, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4ScheduledPatchingV2)
                .substitute(Substitution.ACCOUNTNAME, accountName)
                .substitute(Substitution.START_DATE_TIME, formatDateTime(startTime))
                .substitute(Substitution.END_DATE_TIME, formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES)))
                .substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String accountName, Instant startTime,
                                                             long durationMinutes, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4UnexpectedbutScheduledMaintenanceV2)
                .substitute(Substitution.ACCOUNTNAME, accountName)
                .substitute(Substitution.START_DATE_TIME, formatDateTime(startTime))
                .substitute(Substitution.END_DATE_TIME, formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES)))
                .substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendSystemDownFailoverEmail(String shopperId, String accountName, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4SystemDownFailoverV2)
                .substitute(Substitution.ACCOUNTNAME, accountName)
                .substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendFailoverCompletedEmail(String shopperId, String accountName, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted)
                .substitute(Substitution.ACCOUNTNAME, accountName)
                .substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendUptimeOutageEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                        Instant alertStart, boolean isManaged) {
        ShopperMessage message = buildOutageJson(
                isManaged ? TemplateType.NewFinalManagedUptime : TemplateType.NewFinalSelfManagedUptime,
                accountName, ipAddress, orionGuid, null, null, alertStart, null, isManaged);
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendServerUsageOutageEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                             String resourceName, String resourceUsage, Instant alertStart,
                                             boolean isManaged) {
        ShopperMessage message = buildOutageJson(
                isManaged ? TemplateType.NewFinalManagedServerUsage : TemplateType.NewFinalSelfManagedServerUsage,
                accountName, ipAddress, orionGuid, resourceName, resourceUsage, alertStart, null, isManaged);
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendServicesDownEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                        String serviceName, Instant alertStart, boolean isManaged) {
        ShopperMessage message = buildOutageJson(
                isManaged ? TemplateType.NewFinalManagedServicesDown : TemplateType.NewFinalSelfManagedServicesDown,
                accountName, ipAddress, orionGuid, serviceName, null, alertStart, null, isManaged);
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendUptimeOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                                Instant alertEnd, boolean isManaged) {
        ShopperMessage message =
                buildOutageJson(TemplateType.VPS_DED_4_Issue_Resolved_Uptime, accountName, ipAddress, orionGuid, null,
                                null, null, alertEnd, isManaged);
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendUsageOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                               String resourceName, Instant alertEnd, boolean isManaged) {
        ShopperMessage message =
                buildOutageJson(TemplateType.VPS_DED_4_Issue_Resolved_Resources, accountName, ipAddress, orionGuid,
                                resourceName, null, null, alertEnd, isManaged);
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    @Override
    public String sendServiceOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                                 String serviceName, Instant alertEnd, boolean isManaged) {
        ShopperMessage message =
                buildOutageJson(TemplateType.VPS_DED_4_Issue_Resolved_Services, accountName, ipAddress, orionGuid,
                                serviceName, null, null, alertEnd, isManaged);
        return messagingApiService.sendMessage(shopperId, message).messageId;
    }

    /*
     * The variable metricName is used interchangeably for service name and resource name. The email templates specify
     * a different named parameter for service name and resource name, but we just seem to pass in the metric.
     * We populate the same value in those email parameters, so they get used by the email templates as needed.
     */
    private ShopperMessage buildOutageJson(TemplateType templateType, String accountName,
                                           String ipAddress, UUID orionGuid,
                                           String metricName, String resourceUsage, Instant alertStart,
                                           Instant alertEnd, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(templateType)
                .substitute(Substitution.ACCOUNTNAME, accountName)
                .substitute(Substitution.SERVERNAME, accountName)
                .substitute(Substitution.IPADDRESS, ipAddress)
                .substitute(Substitution.ORION_ID, orionGuid.toString())
                .substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        if (alertStart != null) {
            message.transform(Transformation.ALERTSTARTTIME, alertStart.toString());
        }
        if (alertEnd != null) {
            message.transform(Transformation.ALERTENDTIME, alertEnd.toString());
        }
        if (StringUtils.isNotEmpty(metricName)) {
            message.substitute(Substitution.SERVICENAME, metricName);
        }
        if (StringUtils.isNotEmpty(metricName)) {
            message.substitute(Substitution.RESOURCENAME, metricName);
        }
        if (StringUtils.isNotEmpty(resourceUsage)) {
            message.substitute(Substitution.RESOURCEUSAGE, resourceUsage);
        }
        return message;
    }
}
