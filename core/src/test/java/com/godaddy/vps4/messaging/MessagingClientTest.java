package com.godaddy.vps4.messaging;

import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingMessageId;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessagingClientTest {

    private SecureHttpClient secureHttpClient;
    private Message mockResultMessage;
    private MessagingClient messagingClient;
    private MessagingMessageId mockMessageId;
    private String shopperId;
    private String accountName;
    private String ipAddress;
    private String diskSpace;
    private String baseUrl;

    @Before
    public void setUp() {
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        ipAddress = UUID.randomUUID().toString();
        diskSpace = UUID.randomUUID().toString();
        secureHttpClient = mock(SecureHttpClient.class);
        mockResultMessage = mock(Message.class);
        mockMessageId = mock(MessagingMessageId.class);
        mockMessageId.messageId = UUID.randomUUID().toString();

        baseUrl = UUID.randomUUID().toString();
        messagingClient = new MessagingClient(baseUrl, secureHttpClient);
    }

    @Test
    public void testGetMessageById() {
        String messageId = UUID.randomUUID().toString();

        when(secureHttpClient.executeHttp(Mockito.any(HttpGet.class), Mockito.any())).thenReturn(mockResultMessage);
        Message actualMessage = messagingClient.getMessageById(messageId);
        Assert.assertNotNull(actualMessage);
        Assert.assertThat(mockResultMessage, new ReflectionEquals(actualMessage));
    }

    @Test
    public void testSendSetupEmail() {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingClient.sendSetupEmail(shopperId, accountName, ipAddress, diskSpace);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test
    public void testBuildApiUri() {
        try {
            String uriPath = UUID.randomUUID().toString();
            String expectedResult = String.format("%s%s", baseUrl, uriPath);

            Class[] args = new Class[] {String.class};
            Method buidApiUri = messagingClient.getClass().getDeclaredMethod("buildApiUri", args);
            buidApiUri.setAccessible(true);
            String apiUriResult = (String)buidApiUri.invoke(messagingClient, uriPath);
            Assert.assertEquals(expectedResult, apiUriResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling buildApiUri");
        }
    }

    @Test
    public void testBuildShopperMessageJson() {
        try {
            String accountName = UUID.randomUUID().toString();
            String ipAddress = UUID.randomUUID().toString();
            String diskSpace = UUID.randomUUID().toString();
            ShopperMessage shopperMessage = new ShopperMessage();
            shopperMessage.templateNamespaceKey = MessagingClient.TEMPLATE_NAMESPACE_KEY;
            shopperMessage.templateTypeKey = MessagingClient.EmailTemplates.VirtualPrivateHostingProvisioned4.toString();

            Map<String, String> substitutionValues = new HashMap<>();
            substitutionValues.put(MessagingClient.SetupEmailSubstitutionValues.ACCOUNTNAME.name(), accountName);
            substitutionValues.put(MessagingClient.SetupEmailSubstitutionValues.IPADDRESS.name(), ipAddress);
            substitutionValues.put(MessagingClient.SetupEmailSubstitutionValues.DISKSPACE.name(), diskSpace);
            shopperMessage.substitutionValues = substitutionValues;

            Class[] args = new Class[] {String.class, String.class, String.class};
            Method buildShopperMessageJson = messagingClient.getClass().getDeclaredMethod("buildShopperMessageJson", args);
            buildShopperMessageJson.setAccessible(true);
            String shopperMessageJsonResult = (String)buildShopperMessageJson.invoke(messagingClient,
                    accountName, ipAddress, diskSpace);
            Assert.assertEquals(SecureHttpClient.createJSONFromObject(shopperMessage), shopperMessageJsonResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling buildShopperMessageJson");
        }
    }
}
