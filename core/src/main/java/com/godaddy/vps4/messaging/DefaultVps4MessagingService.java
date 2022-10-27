package com.godaddy.vps4.messaging;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.customer.Customer;
import com.godaddy.vps4.customer.CustomerService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingResponse;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;

public class DefaultVps4MessagingService implements Vps4MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVps4MessagingService.class);

    private final String baseUrl;

    private final CustomerService customerService;
    
    protected final SecureHttpClient client;

    public static final String TEMPLATE_NAMESPACE_KEY = "Hosting";

    public static final String CLIENT_CERTIFICATE_KEY_PATH = "messaging.api.keyPath";

    public static final String CLIENT_CERTIFICATE_PATH = "messaging.api.certPath";

    private final String timezoneForDateParams;

    private final String dateTimePattern;

    public enum EmailTemplates {
        VirtualPrivateHostingProvisioned4,
        VPSWelcomePlesk,
        VPSWelcomeCpanel,
        VPS4ScheduledPatchingV2,
        VPS4UnexpectedbutScheduledMaintenanceV2,
        VPS4SystemDownFailoverV2,
        VPS4UnexpectedscheduledmaintenanceFailoveriscompleted,
        NewFinalSelfManagedUptime,
        NewFinalSelfManagedServerUsage,
        NewFinalSelfManagedServicesDown,
        NewFinalManagedUptime,
        NewFinalManagedServerUsage,
        NewFinalManagedServicesDown,
        VPS_DED_4_Issue_Resolved_Uptime,
        VPS_DED_4_Issue_Resolved_Services,
        VPS_DED_4_Issue_Resolved_Resources
    }

    public enum EmailSubstitutions {
        ACCOUNTNAME,
        IPADDRESS,
        ORION_ID,
        ISMANAGEDSUPPORT,
        START_DATE_TIME,
        END_DATE_TIME,
        ALERTSTARTTIME,
        ALERTENDTIME,
        SERVERNAME,
        SERVICENAME,
        RESOURCENAME,
        RESOURCEUSAGE
    }

    public enum TransformationData {
        ALERTSTARTTIME,
        ALERTENDTIME,
    }

    @Inject
    public DefaultVps4MessagingService(Config config, CustomerService customerService) {
        this(config, new SecureHttpClient(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH), customerService);
    }

    protected DefaultVps4MessagingService(Config config, SecureHttpClient httpClient, CustomerService customerService) {
        this.baseUrl = config.get("messaging.api.url");
        this.timezoneForDateParams = config.get("messaging.timezone");
        this.dateTimePattern = config.get("messaging.datetime.pattern");
        this.client = httpClient;
        this.customerService = customerService;
    }

    protected String buildApiUri(String uriPath) {
        return String.format("%s%s", baseUrl, uriPath);
    }

    @Override
    public Message getMessageById(String messageId) {
        String uriPath = String.format("/v1/messaging/messages/%s", messageId);
        String uri = buildApiUri(uriPath);
        HttpGet httpGet = SecureHttpClient.createJsonHttpGet(uri);

        logger.debug("HTTP GET message id: {} ", messageId);
        try {
            return this.client.executeHttp(httpGet, Message.class);
        } catch (IOException e) {
            logger.error("Exception getting messageId: {} from messaging api: ", messageId, e);
            throw new RuntimeException(e);
        }
    }
    
    private String getShopperId(UUID customerId) {
        Customer customer = customerService.getCustomer(customerId);
        return customer.getShopperId();
    }

    private String buildShopperMessageJson(EmailTemplates template,
            EnumMap<EmailSubstitutions, String> substitutionValues) {
        return buildShopperMessageJson(template, substitutionValues, null);
    }

    private String buildShopperMessageJson(EmailTemplates template,
            EnumMap<EmailSubstitutions, String> substitutionValues,
            EnumMap<TransformationData, String> transformationData) {
        ShopperMessage shopperMessage = new ShopperMessage();
        shopperMessage.templateNamespaceKey = TEMPLATE_NAMESPACE_KEY;
        shopperMessage.templateTypeKey = template.toString();

        shopperMessage.substitutionValues = substitutionValues;

        if (transformationData != null) {
            shopperMessage.transformationData = transformationData;
        }

        return SecureHttpClient.createJSONStringFromObject(shopperMessage);
    }

    @Override
    public String sendSetupEmail(UUID customerId, String accountName, String ipAddress, String orionGuid,
            boolean isManaged) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.IPADDRESS, ipAddress);
        substitutionValues.put(EmailSubstitutions.ORION_ID, orionGuid);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isManaged));

        String shopperMessageJson =
                buildShopperMessageJson(EmailTemplates.VirtualPrivateHostingProvisioned4, substitutionValues);
        return sendMessage(customerId, shopperMessageJson);
    }

    private String sendMessage(UUID customerId, String shopperMessageJson) {
        String shopperId = getShopperId(customerId);
        String uriPath = "/v1/messaging/messages";
        String uri = buildApiUri(uriPath);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Shopper-Id", shopperId);

        logger.debug("JSON POST shopperMessageJson: {} ", shopperMessageJson);
        HttpPost httpPost = SecureHttpClient.createJsonHttpPostWithHeaders(uri, headers);
        httpPost.setEntity(new StringEntity(shopperMessageJson, ContentType.APPLICATION_JSON));

        MessagingResponse response;
        try {
            response = this.client.executeHttp(httpPost, MessagingResponse.class);
            logger.debug("RESPONSE from POST: {} code: {} message: {} messageId: {}", response.toString(),
                    response.code, response.message, response.messageId);
        } catch (IOException e) {
            logger.error("Exception sending to messaging api: ", e);
            throw new RuntimeException(e);
        }

        return response.messageId;
    }

    @Override
    public String sendFullyManagedEmail(UUID customerId, String controlPanel) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        String shopperMessageJson;
        switch (controlPanel.trim().toLowerCase()) {
            case "cpanel":
                shopperMessageJson = buildShopperMessageJson(EmailTemplates.VPSWelcomeCpanel, substitutionValues);
                break;
            case "plesk":
                shopperMessageJson = buildShopperMessageJson(EmailTemplates.VPSWelcomePlesk, substitutionValues);
                break;
            default:
                throw new IllegalArgumentException("Specified control panel not supported for fully managed email.");
        }
        return sendMessage(customerId, shopperMessageJson);

    }

    private String formatDateTime(Instant dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of(this.timezoneForDateParams));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(this.dateTimePattern);

        return dateTimeFormatter.format(zonedDateTime);
    }

    private String buildScheduledMaintenanceJson(EmailTemplates emailTemplate, String accountName, Instant startTime,
            long durationMinutes, boolean isManaged) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        String startDateTime = formatDateTime(startTime);
        substitutionValues.put(EmailSubstitutions.START_DATE_TIME, startDateTime);
        String endDateTime = formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES));
        substitutionValues.put(EmailSubstitutions.END_DATE_TIME, endDateTime);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isManaged));

        return buildShopperMessageJson(emailTemplate, substitutionValues);
    }

    @Override
    public String sendScheduledPatchingEmail(UUID customerId, String accountName, Instant startTime,
            long durationMinutes, boolean isManaged) {
        String shopperMessageJson = buildScheduledMaintenanceJson(EmailTemplates.VPS4ScheduledPatchingV2, accountName,
                startTime, durationMinutes, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendUnexpectedButScheduledMaintenanceEmail(UUID customerId, String accountName, Instant startTime,
            long durationMinutes, boolean isManaged) {
        String shopperMessageJson =
                buildScheduledMaintenanceJson(EmailTemplates.VPS4UnexpectedbutScheduledMaintenanceV2,
                        accountName, startTime, durationMinutes, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    private String buildFailoverJson(EmailTemplates emailTemplate, String accountName, boolean isManaged) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isManaged));

        return buildShopperMessageJson(emailTemplate, substitutionValues);
    }

    @Override
    public String sendSystemDownFailoverEmail(UUID customerId, String accountName, boolean isManaged) {
        String shopperMessageJson = buildFailoverJson(EmailTemplates.VPS4SystemDownFailoverV2, accountName, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendFailoverCompletedEmail(UUID customerId, String accountName, boolean isManaged) {
        String shopperMessageJson =
                buildFailoverJson(EmailTemplates.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted,
                        accountName, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendUptimeOutageEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
            Instant alertStart, boolean isManaged) {
        String shopperMessageJson = buildOutageJson(
                isManaged ? EmailTemplates.NewFinalManagedUptime : EmailTemplates.NewFinalSelfManagedUptime,
                accountName, ipAddress, orionGuid, null, null, alertStart, null, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendServerUsageOutageEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
            String resourceName, String resourceUsage, Instant alertStart,
            boolean isManaged) {
        String shopperMessageJson = buildOutageJson(
                isManaged ? EmailTemplates.NewFinalManagedServerUsage :
                        EmailTemplates.NewFinalSelfManagedServerUsage,
                accountName, ipAddress, orionGuid, resourceName, resourceUsage, alertStart, null, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendServicesDownEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
            String serviceName, Instant alertStart, boolean isManaged) {
        String shopperMessageJson = buildOutageJson(
                isManaged ? EmailTemplates.NewFinalManagedServicesDown :
                        EmailTemplates.NewFinalSelfManagedServicesDown,
                accountName, ipAddress, orionGuid, serviceName, null, alertStart, null, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendUptimeOutageResolvedEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
            Instant alertEnd, boolean isManaged) {
        String shopperMessageJson =
                buildOutageJson(EmailTemplates.VPS_DED_4_Issue_Resolved_Uptime, accountName, ipAddress, orionGuid, null,
                        null, null, alertEnd, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendUsageOutageResolvedEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
            String resourceName, Instant alertEnd, boolean isManaged) {
        String shopperMessageJson =
                buildOutageJson(EmailTemplates.VPS_DED_4_Issue_Resolved_Resources, accountName, ipAddress, orionGuid,
                        resourceName, null, null, alertEnd, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    @Override
    public String sendServiceOutageResolvedEmail(UUID customerId, String accountName, String ipAddress, UUID orionGuid,
            String serviceName, Instant alertEnd, boolean isManaged) {
        String shopperMessageJson =
                buildOutageJson(EmailTemplates.VPS_DED_4_Issue_Resolved_Services, accountName, ipAddress, orionGuid,
                        serviceName, null, null, alertEnd, isManaged);
        return sendMessage(customerId, shopperMessageJson);
    }

    /*
    The variable metricName is used interchangeably for service name and resource name. The email templates specify
     a different named parameter for service name and resource name but we just seem to pass in the metric.
     We populate the same value in those email parameters so they get used by the email templates as needed.
     */
    private String buildOutageJson(EmailTemplates emailTemplate, String accountName, String ipAddress, UUID orionGuid,
            String metricName, String resourceUsage, Instant alertStart, Instant alertEnd, boolean isManaged) {

        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.SERVERNAME, accountName);
        substitutionValues.put(EmailSubstitutions.IPADDRESS, ipAddress);
        substitutionValues.put(EmailSubstitutions.ORION_ID, orionGuid.toString());
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isManaged));
        EnumMap<TransformationData, String> tranformationData = new EnumMap<>(TransformationData.class);
        if (alertStart != null) {
            tranformationData.put(TransformationData.ALERTSTARTTIME, alertStart.toString());
        }
        if (alertEnd != null) {
            tranformationData.put(TransformationData.ALERTENDTIME, alertEnd.toString());
        }
        if (StringUtils.isNotEmpty(metricName)) {
            substitutionValues.put(EmailSubstitutions.SERVICENAME, metricName);
        }
        if (StringUtils.isNotEmpty(metricName)) {
            substitutionValues.put(EmailSubstitutions.RESOURCENAME, metricName);
        }
        if (StringUtils.isNotEmpty(resourceUsage)) {
            substitutionValues.put(EmailSubstitutions.RESOURCEUSAGE, resourceUsage);
        }
        return buildShopperMessageJson(emailTemplate, substitutionValues, tranformationData);
    }
}
