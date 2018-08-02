package com.godaddy.vps4.orchestration.messaging;

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
    ScheduledMaintenanceEmailRequest emailRequest;
    WaitForMessageComplete waitForMsgCompleteCmd;
    Message emailMessage;
    String messageId;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        emailRequest = mock(ScheduledMaintenanceEmailRequest.class);
        emailMessage = mock(Message.class);
        waitForMsgCompleteCmd = new WaitForMessageComplete(messagingService);
    }

    @Test
    public void testExecuteWhenMsgAlreadySucceeded() {
        emailMessage.status = Message.Statuses.SUCCESS.toString();
        when(messagingService.getMessageById(messageId)).thenReturn(emailMessage);

        waitForMsgCompleteCmd.execute(context, messageId);

        verify(messagingService, times(1)).getMessageById(messageId);
    }

    @Test
    public void testExecuteWhenMsgPendingFirst() {
        emailMessage.status = Message.Statuses.PENDING.toString();
        Message successMessage = mock(Message.class);
        successMessage.status = Message.Statuses.SUCCESS.toString();
        when(messagingService.getMessageById(messageId)).thenReturn(emailMessage).thenReturn(successMessage);

        waitForMsgCompleteCmd.execute(context, messageId);

        verify(messagingService, times(2)).getMessageById(messageId);
        verify(context, times(1)).sleep(3000);
    }

    @Test
    public void testExecuteWhenMsgPurged() {
        emailMessage.status = Message.Statuses.PURGED.toString();
        when(messagingService.getMessageById(messageId)).thenReturn(emailMessage);

        waitForMsgCompleteCmd.execute(context, messageId);

        verify(messagingService, times(1)).getMessageById(messageId);
    }

    @Test(expected = NoRetryException.class)
    public void testExecuteWhenMsgFailed() {
        emailMessage.status = Message.Statuses.FAILED.toString();
        emailMessage.failureReason = "Unit test failed message";
        when(messagingService.getMessageById(messageId)).thenReturn(emailMessage);

        waitForMsgCompleteCmd.execute(context, messageId);

        Assert.fail("Expected NoRetryException");
    }

    @Test(expected = NoRetryException.class)
    public void testGetMessageByIdThrowsException() {
        when(messagingService.getMessageById(messageId)).thenThrow(new RuntimeException("Maybe a parse error"));

        waitForMsgCompleteCmd.execute(context, messageId);

        Assert.fail("Expected NoRetryException");
    }
}
