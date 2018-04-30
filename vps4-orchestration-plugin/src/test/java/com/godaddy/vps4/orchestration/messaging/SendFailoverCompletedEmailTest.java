package com.godaddy.vps4.orchestration.messaging;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import gdg.hfs.orchestration.CommandContext;


public class SendFailoverCompletedEmailTest {
    private Vps4MessagingService messagingService;
    private CommandContext context;
    private SendFailoverCompletedEmail sendFailoverCompletedEmailCmd;
    private String messageId;
    private FailOverEmailRequest failOverEmailRequest;
    private String shopperId;
    private String accountName;
    private boolean isFullyManaged;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        sendFailoverCompletedEmailCmd = new SendFailoverCompletedEmail(messagingService);
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        isFullyManaged = false;
        failOverEmailRequest = new FailOverEmailRequest(shopperId, accountName, isFullyManaged);

        when(context.execute(WaitForMessageComplete.class, messageId)).thenReturn(null);
    }

    @Test
    public void testSendFailoverCompletedEmail()  {
        try {
            when(messagingService.sendFailoverCompletedEmail(shopperId, accountName, isFullyManaged))
                    .thenReturn(messageId);

            sendFailoverCompletedEmailCmd.execute(context, failOverEmailRequest);
            verify(messagingService, times(1)).sendFailoverCompletedEmail(shopperId,
                    accountName, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendFailoverCompletedEmailWaitsForMessageComplete()  {
        try {
            when(messagingService.sendFailoverCompletedEmail(shopperId, accountName, isFullyManaged))
                    .thenReturn(messageId);

            sendFailoverCompletedEmailCmd.execute(context, failOverEmailRequest);
            verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testSendFailoverCompletedEmailThrowsRuntimeExceptionForIOEx() {
        try {
            IOException testIOException = mock(IOException.class);
            when(messagingService.sendFailoverCompletedEmail(shopperId, accountName, isFullyManaged))
                    .thenThrow(testIOException);
            sendFailoverCompletedEmailCmd.execute(context, failOverEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testSendFailoverCompletedEmailThrowsRuntimeExceptionForMissingShopperEx() {
        try {
            MissingShopperIdException testMissingEx = mock(MissingShopperIdException.class);
            when(messagingService.sendFailoverCompletedEmail(shopperId, accountName, isFullyManaged))
                    .thenThrow(testMissingEx);
            sendFailoverCompletedEmailCmd.execute(context, failOverEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }
}
