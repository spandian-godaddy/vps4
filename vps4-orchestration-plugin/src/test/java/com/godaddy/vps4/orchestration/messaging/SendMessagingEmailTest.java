package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import gdg.hfs.orchestration.CommandContext;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class SendMessagingEmailTest {
    Vps4MessagingService messagingService;
    CommandContext context;
    EmailRequest emailRequest;
    SendMessagingEmail sendMessagingEmailCmd;
    String messageId;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        emailRequest = mock(EmailRequest.class);
        sendMessagingEmailCmd = new SendMessagingEmail(messagingService);

        when(context.execute(WaitForMessageComplete.class, messageId)).thenReturn(null);
    }

    private void executeSendMessagingEmail() {
        try {
            when(emailRequest.sendEmail(messagingService)).thenReturn(messageId);
            sendMessagingEmailCmd.execute(context, emailRequest);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSendMessagingEmailCallsSendEmail()  {
        try {
            executeSendMessagingEmail();
            verify(emailRequest).sendEmail(messagingService);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendMessagingEmailWaitsForMessageComplete()  {
        executeSendMessagingEmail();
        verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
    }

    @Test(expected = RuntimeException.class)
    public void testSendMessagingEmailThrowsRuntimeExceptionForIOEx() {
        try {
            IOException testIOException = mock(IOException.class);
            when(emailRequest.sendEmail(messagingService)).thenThrow(testIOException);
            sendMessagingEmailCmd.execute(context, emailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testSendMessagingEmailThrowsRuntimeExceptionForMissingShopperEx() {
        try {
            MissingShopperIdException testMissingEx = mock(MissingShopperIdException.class);
            when(emailRequest.sendEmail(messagingService)).thenThrow(testMissingEx);
            sendMessagingEmailCmd.execute(context, emailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testSendMessagingEmailThrowsRuntimeExceptionForNullMessageId() {
        try {
            when(emailRequest.sendEmail(messagingService)).thenReturn(null);
            sendMessagingEmailCmd.execute(context, emailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }
}
