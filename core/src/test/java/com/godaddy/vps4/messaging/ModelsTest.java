package com.godaddy.vps4.messaging;

import com.godaddy.vps4.messaging.models.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class ModelsTest {
    private EmailRecipient emailRecipient;
    private Contact contact;
    private MessagingMessageId messageId;
    private Message message;
    private MessagingEmail email;
    private Preference preference;
    private ShopperMessage shopperMessage;
    private ShopperNote shopperNote;
    private ShopperOverride shopperOverride;
    private String accountName;
    private String ipAddress;
    private String diskSpace;
    private String transformData1;
    private String transformData2;

    private enum UnitTestEnum {
        TEST1,
        TEST2
    }

    @Before
    public void setUp() throws Exception {
        messageId = new MessagingMessageId();
        messageId.messageId = UUID.randomUUID().toString();

        contact = new Contact();
        contact.nameFirst = UUID.randomUUID().toString();
        contact.nameLast = UUID.randomUUID().toString();

        preference = new Preference();
        preference.currency = UUID.randomUUID().toString();
        preference.emailFormat = UUID.randomUUID().toString();
        preference.marketId = UUID.randomUUID().toString();

        emailRecipient = new EmailRecipient();
        emailRecipient.email = UUID.randomUUID().toString();
        emailRecipient.contact = contact;
        emailRecipient.preference = preference;

        shopperOverride = new ShopperOverride();
        shopperOverride.email = UUID.randomUUID().toString();
        shopperOverride.contact = contact;
        shopperOverride.preference = preference;

        shopperNote = new ShopperNote();
        shopperNote.content = UUID.randomUUID().toString();
        shopperNote.enteredBy = UUID.randomUUID().toString();

        shopperMessage = new ShopperMessage();
        shopperMessage.additionalRecipients = new ArrayList<>(Arrays.asList(emailRecipient));
        shopperMessage.sendToShopper = true;
        shopperMessage.shopperNote = shopperNote;
        shopperMessage.shopperOverride = shopperOverride;
        shopperMessage.templateNamespaceKey = UUID.randomUUID().toString();
        shopperMessage.templateTypeKey = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        ipAddress = UUID.randomUUID().toString();
        diskSpace = UUID.randomUUID().toString();

        EnumMap<DefaultVps4MessagingService.EmailSubstitutions, String> substitutionValues =
                new EnumMap<>(DefaultVps4MessagingService.EmailSubstitutions.class);
        substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.ACCOUNTNAME, accountName);
        substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.IPADDRESS, ipAddress);
        substitutionValues.put(DefaultVps4MessagingService.EmailSubstitutions.DISKSPACE, diskSpace);
        shopperMessage.substitutionValues = substitutionValues;

        transformData1 = UUID.randomUUID().toString();
        transformData2 = UUID.randomUUID().toString();
        EnumMap<UnitTestEnum, String> transformationData = new EnumMap<>(UnitTestEnum.class);
        transformationData.put(UnitTestEnum.TEST1, transformData1);
        transformationData.put(UnitTestEnum.TEST2, transformData2);
        shopperMessage.transformationData = transformationData;

        email = new MessagingEmail();
        email.createdAt = UUID.randomUUID().toString();
        email.currency = UUID.randomUUID().toString();
        email.emailFormat = UUID.randomUUID().toString();
        email.emailId = 1;
        email.failureReason = UUID.randomUUID().toString();
        email.marketId = UUID.randomUUID().toString();
        email.status = UUID.randomUUID().toString();
        email.templateId = 2;
        email.to = UUID.randomUUID().toString();

        message = new Message();
        message.shopperId = UUID.randomUUID().toString();
        message.status = UUID.randomUUID().toString();
        message.messageId = UUID.randomUUID().toString();
        message.emails = new ArrayList<>(Arrays.asList(email));
        message.createdAt = UUID.randomUUID().toString();
        message.failureReason = UUID.randomUUID().toString();
        message.privateLabelId = 1;
        message.templateNamespaceKey = UUID.randomUUID().toString();
        message.templateTypeKey = UUID.randomUUID().toString();
    }

    @Test
    public void testMessagingMessageId() {
        String expectedToString = "MessagingMessageId [messageId: " + messageId.messageId + "]";
        Assert.assertEquals(expectedToString, messageId.toString());
    }

    @Test
    public void testContact() {
        String expectedToString = "Contact [nameFirst: " + contact.nameFirst + " nameLast: " + contact.nameLast + "]";
        Assert.assertEquals(expectedToString, contact.toString());
    }

    @Test
    public void testShopperOverride() {
        String expectedToString = "ShopperOverride [email: " + shopperOverride.email +
                " contact: " + shopperOverride.contact + " preference: " + shopperOverride.preference + "]";
        Assert.assertEquals(expectedToString, shopperOverride.toString());
    }

    @Test
    public void testEmailRecipient() {
        String expectedToString = "EmailRecipient [email: " + emailRecipient.email +
                " contact: " + contact.toString() + " preference: " + preference.toString() + "]";
        Assert.assertEquals(expectedToString, emailRecipient.toString());
    }

    @Test
    public void testShopperNote() {
        String expectedToString = "ShopperNote [content: " + shopperNote.content +
                " enteredBy: " + shopperNote.enteredBy + "]";
        Assert.assertEquals(expectedToString, shopperNote.toString());
    }

    @Test
    public void testShopperMessage() {
        String expectedToString = "ShopperMessage [templateNamespaceKey: " + shopperMessage.templateNamespaceKey +
                " templateTypeKey: " + shopperMessage.templateTypeKey +
                " sendToShopper: " + shopperMessage.sendToShopper + " shopperNote: " + shopperNote.toString() +
                " shopperOverride: " + shopperOverride.toString() + "]";
        Assert.assertEquals(expectedToString, shopperMessage.toString());
    }

    @Test
    public void testMessagingEmail() {
        String expectedToString = "MessagingEmail [emailId: " + email.emailId + " templateId: " + email.templateId +
                " to: " + email.to + " status: " + email.status + " createdAt: " + email.createdAt +
                " currency: " + email.currency + " marketId: " + email.marketId +
                " emailFormat: " + email.emailFormat + " failureReason: " + email.failureReason +"]";
        Assert.assertEquals(expectedToString, email.toString());
    }

    @Test
    public void testMessage() {
        String expectedToString = "Message [messageId: " + message.messageId + " status: " + message.status +
                " createdAt: " + message.createdAt + " templateNamespaceKey: " + message.templateNamespaceKey +
                " templateTypeKey: " + message.templateTypeKey + " privateLabelId: " + message.privateLabelId +
                " shopperId: " + message.shopperId + " failureReason: " + message.failureReason + "]";
        Assert.assertEquals(expectedToString, message.toString());
    }
}
