package com.godaddy.vps4.messaging;

import com.godaddy.vps4.cpanel.CpanelClient;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingMessageId;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpGet;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingClient {
    private static final Logger logger = LoggerFactory.getLogger(CpanelClient.class);

    private final String baseUrl;

    protected final SecureHttpClient client;

    public static final String TEMPLATE_NAMESPACE_KEY = "Hosting";

    public static final String CLIENT_CERTIFICATE_KEY_PATH = "messaging.api.keyPath";

    public static final String CLIENT_CERTIFICATE_PATH = "messaging.api.certPath";

    public enum EmailTemplates {
        VirtualPrivateHostingProvisioned4
    }

    public enum SetupEmailSubstitutionValues {
        ACCOUNTNAME,
        IPADDRESS,
        DISKSPACE
    }

    public MessagingClient(String baseUrl, SecureHttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.client = httpClient;
    }

    protected String buildApiUri(String uriPath){
        return String.format("%s%s", baseUrl, uriPath);
    }

    public Message getMessageById(String messageId) {
        String uriPath = String.format("/v1/messaging/messages/%s", messageId);
        String uri = buildApiUri(uriPath);
        HttpGet httpGet = SecureHttpClient.createJsonHttpGet(uri);

        return this.client.executeHttp(httpGet, Message.class);
    }

    private String buildShopperMessageJson(String accountName, String ipAddress, String diskSpace) {
        try {
            ShopperMessage shopperMessage = new ShopperMessage();
            shopperMessage.templateNamespaceKey = TEMPLATE_NAMESPACE_KEY;
            shopperMessage.templateTypeKey = EmailTemplates.VirtualPrivateHostingProvisioned4.toString();

            Map<String, String> substitutionValues = new HashMap<>();
            substitutionValues.put(SetupEmailSubstitutionValues.ACCOUNTNAME.name(), accountName);
            substitutionValues.put(SetupEmailSubstitutionValues.IPADDRESS.name(), ipAddress);
            substitutionValues.put(SetupEmailSubstitutionValues.DISKSPACE.name(), diskSpace);
            shopperMessage.substitutionValues = substitutionValues;

            return SecureHttpClient.createJSONFromObject(shopperMessage);
        }
        catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String sendSetupEmail(String shopperId, String accountName, String ipAddress, String diskSpace) {
        try {
            logger.info(String.format("Sending setup email for shopper: %s", shopperId));
            String uriPath = "/v1/messaging/messages";
            String uri = buildApiUri(uriPath);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Shopper-Id", shopperId);

            HttpPost httpPost = SecureHttpClient.createJsonHttpPostWithHeaders(uri, headers);
            String shopperMessageJson = buildShopperMessageJson(accountName, ipAddress, diskSpace);
            httpPost.setEntity(new StringEntity(shopperMessageJson));
            MessagingMessageId messageId = this.client.executeHttp(httpPost, MessagingMessageId.class);
            logger.info(String.format("Setup email sent for shopper: %s. Message id: %s",
                    shopperId, messageId.messageId));

            return messageId.messageId;
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
