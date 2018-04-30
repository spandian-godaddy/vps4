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


public class SendSystemDownFailoverEmailTest {
    private Vps4MessagingService messagingService;
    private CommandContext context;
    private SendSystemDownFailoverEmail sendSystemDownFailoverEmailCmd;
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
        sendSystemDownFailoverEmailCmd = new SendSystemDownFailoverEmail(messagingService);
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        isFullyManaged = false;
        failOverEmailRequest = new FailOverEmailRequest(shopperId, accountName, isFullyManaged);

        when(context.execute(WaitForMessageComplete.class, messageId)).thenReturn(null);
    }

    @Test
    public void testSendSystemDownFailoverEmail()  {
        try {
            when(messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isFullyManaged))
                    .thenReturn(messageId);

            sendSystemDownFailoverEmailCmd.execute(context, failOverEmailRequest);
            verify(messagingService, times(1)).sendSystemDownFailoverEmail(shopperId,
                    accountName, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendSystemDownFailoverEmailWaitsForMessageComplete()  {
        try {
            when(messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isFullyManaged))
                    .thenReturn(messageId);

            sendSystemDownFailoverEmailCmd.execute(context, failOverEmailRequest);
            verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testSendSystemDownFailoverEmailThrowsRuntimeExceptionForIOEx() {
        try {
            IOException testIOException = mock(IOException.class);
            when(messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isFullyManaged))
                    .thenThrow(testIOException);
            sendSystemDownFailoverEmailCmd.execute(context, failOverEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testSendSystemDownFailoverEmailThrowsRuntimeExceptionForMissingShopperEx() {
        try {
            MissingShopperIdException testMissingEx = mock(MissingShopperIdException.class);
            when(messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isFullyManaged))
                    .thenThrow(testMissingEx);
            sendSystemDownFailoverEmailCmd.execute(context, failOverEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }
}
