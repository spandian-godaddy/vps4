package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.orchestration.NoRetryException;
import gdg.hfs.orchestration.CommandContext;

import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class WaitForMessageCompleteTest {
    Vps4MessagingService messagingService;
    CommandContext context;
    EmailRequest emailRequest;
    WaitForMessageComplete waitForMsgCompleteCmd;
    Message emailMessage;
    String messageId;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        emailRequest = mock(EmailRequest.class);
        emailMessage = mock(Message.class);
        waitForMsgCompleteCmd = new WaitForMessageComplete(messagingService);
    }

    @Test
    public void testExecuteWhenMsgAlreadySucceeded() {
        try {
            emailMessage.status = Message.Statuses.SUCCESS.toString();
            when(messagingService.getMessageById(messageId)).thenReturn(emailMessage);

            waitForMsgCompleteCmd.execute(context, messageId);

            verify(messagingService, times(1)).getMessageById(messageId);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testExecuteWhenMsgPendingFirst() {
        try {
            emailMessage.status = Message.Statuses.PENDING.toString();
            Message successMessage = mock(Message.class);
            successMessage.status = Message.Statuses.SUCCESS.toString();
            when(messagingService.getMessageById(messageId)).thenReturn(emailMessage).thenReturn(successMessage);

            waitForMsgCompleteCmd.execute(context, messageId);

            verify(messagingService, times(2)).getMessageById(messageId);
            verify(context, times(1)).sleep(3000);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testExecuteWhenMsgPurged() {
        try {
            emailMessage.status = Message.Statuses.PURGED.toString();
            when(messagingService.getMessageById(messageId)).thenReturn(emailMessage);

            waitForMsgCompleteCmd.execute(context, messageId);

            verify(messagingService, times(1)).getMessageById(messageId);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = NoRetryException.class)
    public void testExecuteWhenMsgFailed() {
        try {
            emailMessage.status = Message.Statuses.FAILED.toString();
            emailMessage.failureReason = "Unit test failed message";
            when(messagingService.getMessageById(messageId)).thenReturn(emailMessage);

            waitForMsgCompleteCmd.execute(context, messageId);
        }
        catch (IOException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testGetMessageByIdThrowsException() {
        try {
            IOException testIOException = mock(IOException.class);
            when(messagingService.getMessageById(messageId)).thenThrow(testIOException);

            waitForMsgCompleteCmd.execute(context, messageId);
        }
        catch (IOException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }
}
