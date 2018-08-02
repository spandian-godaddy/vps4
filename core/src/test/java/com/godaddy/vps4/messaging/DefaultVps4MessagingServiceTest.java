package com.godaddy.vps4.messaging;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.config.Configs;
import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.messaging.models.MessagingResponse;
import com.godaddy.vps4.messaging.models.ShopperMessage;
import com.godaddy.vps4.util.SecureHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultVps4MessagingServiceTest {

    private SecureHttpClient secureHttpClient;
    private Message mockResultMessage;
    private DefaultVps4MessagingService messagingService;
    private MessagingResponse mockMessageId;
    private String shopperId;
    private String accountName;
    private String ipAddress;
    private String orionId;
    private Boolean isFullyManaged;
    private Config config;
    private Instant startTime;
    private long durationMinutes;

    @Before
    public void setUp() {
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        ipAddress = UUID.randomUUID().toString();
        orionId = UUID.randomUUID().toString();
        isFullyManaged = false;
        startTime = Instant.now();
        durationMinutes = 1440;
        secureHttpClient = mock(SecureHttpClient.class);
        mockResultMessage = mock(Message.class);
        mockMessageId = mock(MessagingResponse.class);
        mockMessageId.messageId = UUID.randomUUID().toString();

        config = Configs.getInstance();
        messagingService = new DefaultVps4MessagingService(config, secureHttpClient);
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
    public void testSendFullyManagedEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendFullyManagedEmail(shopperId, "cpanel");
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test
    public void testSendMessage() {
        try {
            String shopperId = UUID.randomUUID().toString();
            String shopperMessageJson = UUID.randomUUID().toString();

            when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
            Class[] args = new Class[]{String.class, String.class};
            Method sendMessage = messagingService.getClass().getDeclaredMethod("sendMessage", args);
            sendMessage.setAccessible(true);

            String actualResult = (String) sendMessage.invoke(messagingService, shopperId, shopperMessageJson);
            Assert.assertEquals(mockMessageId.messageId, actualResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling sendMessage");
        }
    }

    @Test
    public void testSendSetupEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendSetupEmail(shopperId, accountName, ipAddress,
                orionId, isFullyManaged);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test
    public void testBuildApiUri() {
        try {
            String baseUrl = config.get("messaging.api.url");
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
            String orionId = UUID.randomUUID().toString();
            Boolean isFullyManaged = false;
            ShopperMessage shopperMessage = new ShopperMessage();
            shopperMessage.templateNamespaceKey = DefaultVps4MessagingService.TEMPLATE_NAMESPACE_KEY;
            shopperMessage.templateTypeKey = DefaultVps4MessagingService.EmailTemplates.VirtualPrivateHostingProvisioned4.toString();

            EnumMap<DefaultVps4MessagingService.EmailSubstitutions, String> substitutionValues =
                    new EnumMap<>(DefaultVps4MessagingService.EmailSubstitutions.class);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ACCOUNTNAME, accountName);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.IPADDRESS, ipAddress);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ORION_ID, orionId);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ISMANAGEDSUPPORT,
                    Boolean.toString(isFullyManaged));
            shopperMessage.substitutionValues = substitutionValues;

            Class[] args = new Class[] {EmailTemplates.class, EnumMap.class};
            Method buildShopperMessageJson = messagingService.getClass().getDeclaredMethod("buildShopperMessageJson", args);
            buildShopperMessageJson.setAccessible(true);
            String shopperMessageJsonResult = (String)buildShopperMessageJson.invoke(messagingService,
                    DefaultVps4MessagingService.EmailTemplates.VirtualPrivateHostingProvisioned4, substitutionValues);
            Assert.assertEquals(SecureHttpClient.createJSONStringFromObject(shopperMessage), shopperMessageJsonResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling buildShopperMessageJson");
        }
    }

    private Method getFormatDateTimeMethod() throws NoSuchMethodException {
        Class[] args = new Class[] { Instant.class };
        Method formatDateTime = messagingService.getClass().getDeclaredMethod("formatDateTime", args);
        formatDateTime.setAccessible(true);

        return formatDateTime;
    }

    @Test
    public void testFormatDateTime() {
        try {
            Instant dateTimeTarget = Instant.now();

            Method formatDateTime = getFormatDateTimeMethod();
            String actualResult = (String)formatDateTime.invoke(messagingService, dateTimeTarget);

            ZonedDateTime zonedDateTime = dateTimeTarget.atZone(ZoneId.of(config.get("messaging.timezone")));
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(config.get("messaging.datetime.pattern"));
            String expectedResult =  dateTimeFormatter.format(zonedDateTime);

            Assert.assertEquals(expectedResult, actualResult);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling formatDateTime");
        }
    }

    @Test
    public void testBuildScheduledMaintenanceJson() {
        try {
            EmailTemplates emailTemplate = EmailTemplates.VPS4ScheduledPatchingV2;
            String accountName = UUID.randomUUID().toString();
            Instant startTime = Instant.now();
            long durationMinutes = 1440;
            Boolean isFullyManaged = false;
            Method formatDateTime = getFormatDateTimeMethod();
            ShopperMessage shopperMessage = new ShopperMessage();
            shopperMessage.templateNamespaceKey = DefaultVps4MessagingService.TEMPLATE_NAMESPACE_KEY;
            shopperMessage.templateTypeKey = EmailTemplates.VPS4ScheduledPatchingV2.toString();

            EnumMap<DefaultVps4MessagingService.EmailSubstitutions, String> substitutionValues =
                    new EnumMap<>(DefaultVps4MessagingService.EmailSubstitutions.class);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ACCOUNTNAME, accountName);
            String startDateTime = (String)formatDateTime.invoke(messagingService, startTime);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.START_DATE_TIME, startDateTime);
            String endDateTime = (String)formatDateTime.invoke(messagingService,
                    startTime.plus(durationMinutes, ChronoUnit.MINUTES));
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.END_DATE_TIME, endDateTime);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ISMANAGEDSUPPORT,
                    Boolean.toString(isFullyManaged));
            shopperMessage.substitutionValues = substitutionValues;

            Class[] args = new Class[] {EmailTemplates.class, String.class, Instant.class, long.class, boolean.class};
            Method buildScheduledMaintenanceJson = messagingService.getClass().getDeclaredMethod("buildScheduledMaintenanceJson", args);
            buildScheduledMaintenanceJson.setAccessible(true);
            String shopperMessageJsonResult = (String)buildScheduledMaintenanceJson.invoke(messagingService,
                    emailTemplate, accountName, startTime, durationMinutes, isFullyManaged);
            Assert.assertEquals(SecureHttpClient.createJSONStringFromObject(shopperMessage), shopperMessageJsonResult);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling buildScheduledMaintenanceJson");
        }
    }

    @Test
    public void testSendScheduledPatchingEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendScheduledPatchingEmail(shopperId, accountName, startTime,
                durationMinutes, isFullyManaged);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test
    public void testSendUnexpectedButScheduledMaintenanceEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName,
                startTime, durationMinutes, isFullyManaged);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test
    public void testBuildFailoverJson() {
        try {
            EmailTemplates emailTemplate = EmailTemplates.VPS4SystemDownFailoverV2;
            String accountName = UUID.randomUUID().toString();
            Boolean isFullyManaged = false;
            ShopperMessage shopperMessage = new ShopperMessage();
            shopperMessage.templateNamespaceKey = DefaultVps4MessagingService.TEMPLATE_NAMESPACE_KEY;
            shopperMessage.templateTypeKey = EmailTemplates.VPS4SystemDownFailoverV2.toString();

            EnumMap<DefaultVps4MessagingService.EmailSubstitutions, String> substitutionValues =
                    new EnumMap<>(DefaultVps4MessagingService.EmailSubstitutions.class);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ACCOUNTNAME, accountName);
            substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ISMANAGEDSUPPORT,
                    Boolean.toString(isFullyManaged));
            shopperMessage.substitutionValues = substitutionValues;

            Class[] args = new Class[] {EmailTemplates.class, String.class, boolean.class};
            Method buildFailoverJson = messagingService.getClass().getDeclaredMethod("buildFailoverJson", args);
            buildFailoverJson.setAccessible(true);
            String shopperMessageJsonResult = (String)buildFailoverJson.invoke(messagingService,
                    emailTemplate, accountName, isFullyManaged);
            Assert.assertEquals(SecureHttpClient.createJSONStringFromObject(shopperMessage), shopperMessageJsonResult);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Failed in calling buildFailoverJson");
        }
    }

    @Test
    public void testSendSystemDownFailoverEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isFullyManaged);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }

    @Test
    public void testSendFailoverCompletedEmail() throws MissingShopperIdException, IOException {
        when(secureHttpClient.executeHttp(Mockito.any(HttpPost.class), Mockito.any())).thenReturn(mockMessageId);
        String actualMessageId = messagingService.sendFailoverCompletedEmail(shopperId, accountName, isFullyManaged);
        Assert.assertNotNull(actualMessageId);
        Assert.assertEquals(mockMessageId.messageId, actualMessageId);
    }
}