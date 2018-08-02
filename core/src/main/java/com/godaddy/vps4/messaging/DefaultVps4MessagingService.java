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

import javax.inject.Inject;

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
        VPS4UnexpectedscheduledmaintenanceFailoveriscompleted
    }

    public enum EmailSubstitutions {
        ACCOUNTNAME,
        IPADDRESS,
        ORION_ID,
        ISMANAGEDSUPPORT,
        START_DATE_TIME,
        END_DATE_TIME
    }

    @Inject
    public DefaultVps4MessagingService (Config config) {
        this(config, new SecureHttpClient(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH));
    }

    protected DefaultVps4MessagingService(Config config, SecureHttpClient httpClient) {
        this.baseUrl = config.get("messaging.api.url");
        this.timezoneForDateParams = config.get("messaging.timezone");
        this.dateTimePattern = config.get("messaging.datetime.pattern");
        this.client = httpClient;
    }

    protected String buildApiUri(String uriPath){
        return String.format("%s%s", baseUrl, uriPath);
    }

    @Override
    public Message getMessageById(String messageId) {
        String uriPath = String.format("/v1/messaging/messages/%s", messageId);
        String uri = buildApiUri(uriPath);
        HttpGet httpGet = SecureHttpClient.createJsonHttpGet(uri);

        try {
            return this.client.executeHttp(httpGet, Message.class);
        } catch (IOException e) {
            logger.error("Exception getting messageId: {} from messaging api: ", messageId, e);
            throw new RuntimeException(e);
        }
    }

    private String buildShopperMessageJson(EmailTemplates template, EnumMap<EmailSubstitutions, String> substitutionValues) {
        ShopperMessage shopperMessage = new ShopperMessage();
        shopperMessage.templateNamespaceKey = TEMPLATE_NAMESPACE_KEY;
        shopperMessage.templateTypeKey = template.toString();

        shopperMessage.substitutionValues = substitutionValues;

        return SecureHttpClient.createJSONStringFromObject(shopperMessage);
    }

    @Override
    public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionGuid,
                                 boolean isFullyManaged) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.IPADDRESS, ipAddress);
        substitutionValues.put(EmailSubstitutions.ORION_ID, orionGuid);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isFullyManaged));

        String shopperMessageJson = buildShopperMessageJson(EmailTemplates.VirtualPrivateHostingProvisioned4, substitutionValues);
        return sendMessage(shopperId, shopperMessageJson);
    }

    private String sendMessage(String shopperId, String shopperMessageJson) {
        String uriPath = "/v1/messaging/messages";
        String uri = buildApiUri(uriPath);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Shopper-Id", shopperId);

        HttpPost httpPost = SecureHttpClient.createJsonHttpPostWithHeaders(uri, headers);
        httpPost.setEntity(new StringEntity(shopperMessageJson, ContentType.APPLICATION_JSON));

        MessagingResponse response;
        try {
            response = this.client.executeHttp(httpPost, MessagingResponse.class);
        } catch (IOException e) {
            logger.error("Exception sending to messaging api: ", e);
            throw new RuntimeException(e);
        }

        return response.messageId;
    }

    @Override
    public String sendFullyManagedEmail(String shopperId, String controlPanel) throws MissingShopperIdException, IOException {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        String shopperMessageJson = null;
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
        return sendMessage(shopperId, shopperMessageJson);

    }

    private String formatDateTime(Instant dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of(this.timezoneForDateParams));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(this.dateTimePattern);

        return dateTimeFormatter.format(zonedDateTime);
    }

    private String buildScheduledMaintenanceJson(EmailTemplates emailTemplate, String accountName, Instant startTime,
                                                 long durationMinutes, boolean isFullyManaged) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        String startDateTime = formatDateTime(startTime);
        substitutionValues.put(EmailSubstitutions.START_DATE_TIME, startDateTime);
        String endDateTime = formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES));
        substitutionValues.put(EmailSubstitutions.END_DATE_TIME, endDateTime);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isFullyManaged));

        return buildShopperMessageJson(emailTemplate, substitutionValues);
    }

    @Override
    public String sendScheduledPatchingEmail(String shopperId, String accountName, Instant startTime,
                                             long durationMinutes, boolean isFullyManaged) {
        String shopperMessageJson = buildScheduledMaintenanceJson(EmailTemplates.VPS4ScheduledPatchingV2, accountName,
                startTime, durationMinutes, isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }

    @Override
    public String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String accountName, Instant startTime,
                                                             long durationMinutes, boolean isFullyManaged) {
        String shopperMessageJson = buildScheduledMaintenanceJson(EmailTemplates.VPS4UnexpectedbutScheduledMaintenanceV2,
                accountName, startTime, durationMinutes, isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }

    private String buildFailoverJson(EmailTemplates emailTemplate, String accountName, boolean isFullyManaged) {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isFullyManaged));

        return buildShopperMessageJson(emailTemplate, substitutionValues);
    }

    @Override
    public String sendSystemDownFailoverEmail(String shopperId, String accountName, boolean isFullyManaged) {
        String shopperMessageJson = buildFailoverJson(EmailTemplates.VPS4SystemDownFailoverV2, accountName,
                isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }

    @Override
    public String sendFailoverCompletedEmail(String shopperId, String accountName, boolean isFullyManaged) {
        String shopperMessageJson = buildFailoverJson(EmailTemplates.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted,
                accountName, isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }
}
