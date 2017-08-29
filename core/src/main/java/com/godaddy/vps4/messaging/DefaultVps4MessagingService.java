package com.godaddy.vps4.messaging;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingMessageId;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;

import java.util.EnumMap;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import javax.inject.Inject;

public class DefaultVps4MessagingService implements Vps4MessagingService {

    private final String baseUrl;

    protected final SecureHttpClient client;

    public static final String TEMPLATE_NAMESPACE_KEY = "Hosting";

    public static final String CLIENT_CERTIFICATE_KEY_PATH = "messaging.api.keyPath";

    public static final String CLIENT_CERTIFICATE_PATH = "messaging.api.certPath";

    public enum EmailTemplates {
        VirtualPrivateHostingProvisioned4
    }

    public enum EmailSubstitutions {
        ACCOUNTNAME,
        IPADDRESS,
        DISKSPACE
    }

    @Inject
    public DefaultVps4MessagingService (Config config) {
        this(config.get("messaging.api.url"), new SecureHttpClient(
                config,
                CLIENT_CERTIFICATE_KEY_PATH,
                CLIENT_CERTIFICATE_PATH));
    }

    protected DefaultVps4MessagingService(String baseUrl, SecureHttpClient httpClient) {
        this.baseUrl = baseUrl;
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

    private String buildShopperMessageJson(String accountName, String ipAddress, String diskSpace)
            throws JsonProcessingException {
        ShopperMessage shopperMessage = new ShopperMessage();
        shopperMessage.templateNamespaceKey = TEMPLATE_NAMESPACE_KEY;
        shopperMessage.templateTypeKey = EmailTemplates.VirtualPrivateHostingProvisioned4.toString();
        EnumMap<EmailSubstitutions, String> substitutionValues = new EnumMap<>(EmailSubstitutions.class);
        substitutionValues.put(EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(EmailSubstitutions.IPADDRESS, ipAddress);
        substitutionValues.put(EmailSubstitutions.DISKSPACE, diskSpace);
        shopperMessage.substitutionValues = substitutionValues;

        return SecureHttpClient.createJSONFromObject(shopperMessage);
    }

    private void VerifyShopperId(String shopperId) throws MissingShopperIdException {
        if (shopperId == null || shopperId.isEmpty()) {
            throw new MissingShopperIdException("Shopper id is a required parameter.");
        }
    }

    public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String diskSpace)
            throws MissingShopperIdException, IOException {
        VerifyShopperId(shopperId);
        String uriPath = "/v1/messaging/messages";
        String uri = buildApiUri(uriPath);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Shopper-Id", shopperId);

        HttpPost httpPost = SecureHttpClient.createJsonHttpPostWithHeaders(uri, headers);
        String shopperMessageJson = buildShopperMessageJson(accountName, ipAddress, diskSpace);
        httpPost.setEntity(new StringEntity(shopperMessageJson));
        MessagingMessageId messageId = this.client.executeHttp(httpPost, MessagingMessageId.class);

        return messageId.messageId;
    }
}
