package com.godaddy.vps4.messaging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.messaging.models.Substitution;
import com.godaddy.vps4.messaging.models.TemplateType;
import com.godaddy.vps4.messaging.models.Transformation;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.clients.Vps4SsoMessagingApi;
import com.godaddy.vps4.sso.models.Vps4SsoToken;

public class DefaultMessagingService implements MessagingService {
    private static final String CACHE_KEY = "messaging-api";

    private final DateTimeFormatter dateTimeFormatter;
    private final Cache<String, String> cache;
    private final MessagingApiService messagingApiService;
    private final Vps4SsoService vps4SsoService;

    @Inject
    public DefaultMessagingService(CacheManager cacheManager,
                                   MessagingApiService messagingApiService,
                                   @Vps4SsoMessagingApi Vps4SsoService vps4SsoService) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.cache = cacheManager.getCache(CacheName.API_ACCESS_TOKENS, String.class, String.class);
        this.messagingApiService = messagingApiService;
        this.vps4SsoService = vps4SsoService;
    }

    private String getAuth() {
        if (cache.containsKey(CACHE_KEY)) {
            return "sso-jwt " + cache.get(CACHE_KEY);
        }
        Vps4SsoToken token = vps4SsoService.getToken("cert");
        cache.put(CACHE_KEY, token.value());
        return "sso-jwt " + token.value();
    }

    @Override
    public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionGuid,
                                 boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VirtualPrivateHostingProvisioned4);
        message.substitute(Substitution.ACCOUNTNAME, accountName);
        message.substitute(Substitution.IPADDRESS, ipAddress);
        message.substitute(Substitution.ORION_ID, orionGuid);
        message.substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
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
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    private String formatDateTime(Instant dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("GMT"));
        return dateTimeFormatter.format(zonedDateTime);
    }

    @Override
    public String sendScheduledPatchingEmail(String shopperId, String accountName, Instant startTime,
                                             long durationMinutes, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4ScheduledPatchingV2);
        message.substitute(Substitution.ACCOUNTNAME, accountName);
        message.substitute(Substitution.START_DATE_TIME, formatDateTime(startTime));
        message.substitute(Substitution.END_DATE_TIME, formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES)));
        message.substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String accountName, Instant startTime,
                                                             long durationMinutes, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4UnexpectedbutScheduledMaintenanceV2);
        message.substitute(Substitution.ACCOUNTNAME, accountName);
        message.substitute(Substitution.START_DATE_TIME, formatDateTime(startTime));
        message.substitute(Substitution.END_DATE_TIME, formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES)));
        message.substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendSystemDownFailoverEmail(String shopperId, String accountName, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4SystemDownFailoverV2);
        message.substitute(Substitution.ACCOUNTNAME, accountName);
        message.substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendFailoverCompletedEmail(String shopperId, String accountName, boolean isManaged) {
        ShopperMessage message = new ShopperMessage(TemplateType.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted);
        message.substitute(Substitution.ACCOUNTNAME, accountName);
        message.substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendUptimeOutageEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                        Instant alertStart, boolean isManaged) {
        ShopperMessage message = buildOutageJson(
                isManaged ? TemplateType.NewFinalManagedUptime : TemplateType.NewFinalSelfManagedUptime,
                accountName, ipAddress, orionGuid, null, null, alertStart, null, isManaged);
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendServerUsageOutageEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                             String resourceName, String resourceUsage, Instant alertStart,
                                             boolean isManaged) {
        ShopperMessage message = buildOutageJson(
                isManaged ? TemplateType.NewFinalManagedServerUsage : TemplateType.NewFinalSelfManagedServerUsage,
                accountName, ipAddress, orionGuid, resourceName, resourceUsage, alertStart, null, isManaged);
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendServicesDownEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                        String serviceName, Instant alertStart, boolean isManaged) {
        ShopperMessage message = buildOutageJson(
                isManaged ? TemplateType.NewFinalManagedServicesDown : TemplateType.NewFinalSelfManagedServicesDown,
                accountName, ipAddress, orionGuid, serviceName, null, alertStart, null, isManaged);
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendUptimeOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                                Instant alertEnd, boolean isManaged) {
        ShopperMessage message =
                buildOutageJson(TemplateType.VPS_DED_4_Issue_Resolved_Uptime, accountName, ipAddress, orionGuid, null,
                                null, null, alertEnd, isManaged);
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendUsageOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                               String resourceName, Instant alertEnd, boolean isManaged) {
        ShopperMessage message =
                buildOutageJson(TemplateType.VPS_DED_4_Issue_Resolved_Resources, accountName, ipAddress, orionGuid,
                                resourceName, null, null, alertEnd, isManaged);
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
    }

    @Override
    public String sendServiceOutageResolvedEmail(String shopperId, String accountName, String ipAddress, UUID orionGuid,
                                                 String serviceName, Instant alertEnd, boolean isManaged) {
        ShopperMessage message =
                buildOutageJson(TemplateType.VPS_DED_4_Issue_Resolved_Services, accountName, ipAddress, orionGuid,
                                serviceName, null, null, alertEnd, isManaged);
        return messagingApiService.sendMessage(getAuth(), shopperId, message).messageId;
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
        ShopperMessage message = new ShopperMessage(templateType);
        message.substitute(Substitution.ACCOUNTNAME, accountName);
        message.substitute(Substitution.SERVERNAME, accountName);
        message.substitute(Substitution.IPADDRESS, ipAddress);
        message.substitute(Substitution.ORION_ID, orionGuid.toString());
        message.substitute(Substitution.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
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
