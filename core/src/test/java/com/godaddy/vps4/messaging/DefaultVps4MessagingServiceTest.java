package com.godaddy.vps4.messaging;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingMessageId;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultVps4MessagingServiceTest {

    private SecureHttpClient secureHttpClient;
    private Message mockResultMessage;
    private DefaultVps4MessagingService messagingService;
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
        messagingService = new DefaultVps4MessagingService(baseUrl, secureHttpClient);
    }

    @Test
    public void testGetMessageById() throws IOException {
        String messageId = UUID.randomUUID().toString();

        when(secureHttpClient.executeHttp(Mockito.any(HttpGet.class), Mockito.any())).thenReturn(mockResultMessage);
        Message actualMessage = messagingService.getMessageById(messageId);
        Assert.assertNotNull(actualMessage);
        Assert.assertThat(mockResultMessage, new ReflectionEquals(actualMessage));
    }

    @Test
    public void testSendSetupEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendSetupEmail(shopperId, accountName, ipAddress, diskSpace);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test(expected = MissingShopperIdException.class)
    public void testSendSetupEmailThrowsMissingShopperIdExceptionWhenEmpty() throws MissingShopperIdException, IOException {
        messagingService.sendSetupEmail("", accountName, ipAddress, diskSpace);
    }

    @Test(expected = MissingShopperIdException.class)
    public void testSendSetupEmailThrowsMissingShopperIdExceptionWhenNull() throws MissingShopperIdException, IOException {
        messagingService.sendSetupEmail(null, accountName, ipAddress, diskSpace);
    }

    @Test
    public void testBuildApiUri() {
        try {
            String uriPath = UUID.randomUUID().toString();
            String expectedResult = String.format("%s%s", baseUrl, uriPath);

            Class[] args = new Class[] {String.class};
            Method buidApiUri = messagingService.getClass().getDeclaredMethod("buildApiUri", args);
            buidApiUri.setAccessible(true);
            String apiUriResult = (String)buidApiUri.invoke(messagingService, uriPath);
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
            shopperMessage.templateNamespaceKey = DefaultVps4MessagingService.TEMPLATE_NAMESPACE_KEY;
            shopperMessage.templateTypeKey = DefaultVps4MessagingService.EmailTemplates.VirtualPrivateHostingProvisioned4.toString();

            EnumMap<DefaultVps4MessagingService.EmailSubstitutions, String> substitutionValues =
                    new EnumMap<>(DefaultVps4MessagingService.EmailSubstitutions.class);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ACCOUNTNAME, accountName);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.IPADDRESS, ipAddress);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.DISKSPACE, diskSpace);
            shopperMessage.substitutionValues = substitutionValues;

            Class[] args = new Class[] {String.class, String.class, String.class};
            Method buildShopperMessageJson = messagingService.getClass().getDeclaredMethod("buildShopperMessageJson", args);
            buildShopperMessageJson.setAccessible(true);
            String shopperMessageJsonResult = (String)buildShopperMessageJson.invoke(messagingService,
                    accountName, ipAddress, diskSpace);
            Assert.assertEquals(SecureHttpClient.createJSONFromObject(shopperMessage), shopperMessageJsonResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling buildShopperMessageJson");
        }
    }
}
