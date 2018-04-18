package com.godaddy.vps4.messaging;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.time.ZoneId;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingMessageId;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;

public class DefaultVps4MessagingService implements Vps4MessagingService {

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
        VPS4ScheduledPatching,
        VPS4UnexpectedbutScheduledMaintenance,
        VPS4SystemDownFailover,
        VPS4UnexpectedschedmaintFailoveriscompleted
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

    public Message getMessageById(String messageId) throws IOException {
        String uriPath = String.format("/v1/messaging/messages/%s", messageId);
        String uri = buildApiUri(uriPath);
        HttpGet httpGet = SecureHttpClient.createJsonHttpGet(uri);

        return this.client.executeHttp(httpGet, Message.class);
    }

    private String buildShopperMessageJson(EmailTemplates template, EnumMap<EmailSubstitutions, String> substitutionValues)
            throws JsonProcessingException {
        ShopperMessage shopperMessage = new ShopperMessage();
        shopperMessage.templateNamespaceKey = TEMPLATE_NAMESPACE_KEY;
        shopperMessage.templateTypeKey = template.toString();

        shopperMessage.substitutionValues = substitutionValues;

        return SecureHttpClient.createJSONFromObject(shopperMessage);
    }

    private void VerifyShopperId(String shopperId) throws MissingShopperIdException {
        if (shopperId == null || shopperId.isEmpty()) {
            throw new MissingShopperIdException("Shopper id is a required parameter.");
        }
    }

    public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String orionGuid,
                                 boolean isFullyManaged)
            throws MissingShopperIdException, IOException {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.IPADDRESS, ipAddress);
        substitutionValues.put(EmailSubstitutions.ORION_ID, orionGuid);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isFullyManaged));

        String shopperMessageJson = buildShopperMessageJson(EmailTemplates.VirtualPrivateHostingProvisioned4, substitutionValues);
        return sendMessage(shopperId, shopperMessageJson);
    }

    private String sendMessage(String shopperId, String shopperMessageJson)
            throws MissingShopperIdException, UnsupportedEncodingException, IOException {
        VerifyShopperId(shopperId);
        String uriPath = "/v1/messaging/messages";
        String uri = buildApiUri(uriPath);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Shopper-Id", shopperId);

        HttpPost httpPost = SecureHttpClient.createJsonHttpPostWithHeaders(uri, headers);
        httpPost.setEntity(new StringEntity(shopperMessageJson));
        MessagingMessageId messageId = this.client.executeHttp(httpPost, MessagingMessageId.class);

        return messageId.messageId;
    }

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
                                                 long durationMinutes, boolean isFullyManaged) throws IOException {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        String startDateTime = formatDateTime(startTime);
        substitutionValues.put(EmailSubstitutions.START_DATE_TIME, startDateTime);
        String endDateTime = formatDateTime(startTime.plus(durationMinutes, ChronoUnit.MINUTES));
        substitutionValues.put(EmailSubstitutions.END_DATE_TIME, endDateTime);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isFullyManaged));

        return buildShopperMessageJson(emailTemplate, substitutionValues);
    }

    public String sendScheduledPatchingEmail(String shopperId, String accountName, Instant startTime,
                                             long durationMinutes, boolean isFullyManaged)
            throws MissingShopperIdException, IOException {
        String shopperMessageJson = buildScheduledMaintenanceJson(EmailTemplates.VPS4ScheduledPatching, accountName,
                startTime, durationMinutes, isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }

    public String sendUnexpectedButScheduledMaintenanceEmail(String shopperId, String accountName, Instant startTime,
                                                             long durationMinutes, boolean isFullyManaged)
            throws MissingShopperIdException, IOException {
        String shopperMessageJson = buildScheduledMaintenanceJson(EmailTemplates.VPS4UnexpectedbutScheduledMaintenance,
                accountName, startTime, durationMinutes, isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }

    private String buildFailoverJson(EmailTemplates emailTemplate, String accountName, boolean isFullyManaged)
            throws IOException {
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.ISMANAGEDSUPPORT, Boolean.toString(isFullyManaged));

        return buildShopperMessageJson(emailTemplate, substitutionValues);
    }

    public String sendSystemDownFailoverEmail(String shopperId, String accountName, boolean isFullyManaged)
            throws MissingShopperIdException, IOException {
        String shopperMessageJson = buildFailoverJson(EmailTemplates.VPS4SystemDownFailover, accountName,
                isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }

    public String sendFailoverCompletedEmail(String shopperId, String accountName, boolean isFullyManaged)
            throws MissingShopperIdException, IOException {
        String shopperMessageJson = buildFailoverJson(EmailTemplates.VPS4UnexpectedschedmaintFailoveriscompleted,
                accountName, isFullyManaged);

        return sendMessage(shopperId, shopperMessageJson);
    }
}
