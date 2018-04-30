package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.MissingShopperIdException;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class SendUnexpectedButScheduledMaintenanceEmailTest {
    private Vps4MessagingService messagingService;
    private CommandContext context;
    private SendUnexpectedButScheduledMaintenanceEmail sendUnexpectedButScheduledMaintenanceEmailCmd;
    private String messageId;
    private ScheduledMaintenanceEmailRequest scheduledMaintenanceEmailRequest;
    private String shopperId;
    private String accountName;
    private boolean isFullyManaged;
    private Instant startTime;
    private long durationMinutes;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        sendUnexpectedButScheduledMaintenanceEmailCmd = new SendUnexpectedButScheduledMaintenanceEmail(messagingService);
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        isFullyManaged = false;
        startTime = Instant.now();
        durationMinutes = Long.MAX_VALUE;
        scheduledMaintenanceEmailRequest = new ScheduledMaintenanceEmailRequest(
                shopperId, accountName, isFullyManaged, startTime, durationMinutes);

        when(context.execute(WaitForMessageComplete.class, messageId)).thenReturn(null);
    }

    @Test
    public void testSendUnexpectedButScheduledMaintenanceEmail()  {
        try {
            when(messagingService.sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName, startTime,
                    durationMinutes, isFullyManaged)).thenReturn(messageId);

            sendUnexpectedButScheduledMaintenanceEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
            verify(messagingService, times(1)).sendUnexpectedButScheduledMaintenanceEmail(
                    shopperId, accountName, startTime, durationMinutes, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendUnexpectedButScheduledMaintenanceEmailWaitsForMessageComplete()  {
        try {
            when(messagingService.sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName, startTime,
                    durationMinutes, isFullyManaged)).thenReturn(messageId);

            sendUnexpectedButScheduledMaintenanceEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
            verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testSendUnexpectedButScheduledMaintenanceEmailThrowsRuntimeExceptionForIOEx() {
        try {
            IOException testIOException = mock(IOException.class);
            when(messagingService.sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName, startTime,
                    durationMinutes, isFullyManaged)).thenThrow(testIOException);
            sendUnexpectedButScheduledMaintenanceEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testSendUnexpectedButScheduledMaintenanceEmailThrowsRuntimeExceptionForMissingShopperEx() {
        try {
            MissingShopperIdException testMissingEx = mock(MissingShopperIdException.class);
            when(messagingService.sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName, startTime,
                    durationMinutes, isFullyManaged)).thenThrow(testMissingEx);
            sendUnexpectedButScheduledMaintenanceEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }
}
