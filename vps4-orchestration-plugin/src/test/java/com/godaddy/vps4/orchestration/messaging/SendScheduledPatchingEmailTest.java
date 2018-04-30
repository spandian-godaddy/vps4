package com.godaddy.vps4.orchestration.messaging;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import gdg.hfs.orchestration.CommandContext;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.MissingShopperIdException;

public class SendScheduledPatchingEmailTest {
    private Vps4MessagingService messagingService;
    private CommandContext context;
    private SendScheduledPatchingEmail sendScheduledPatchingEmailCmd;
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
        sendScheduledPatchingEmailCmd = new SendScheduledPatchingEmail(messagingService);
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
    public void testSendScheduledPatchingEmail()  {
        try {
            when(messagingService.sendScheduledPatchingEmail(shopperId, accountName, startTime, durationMinutes,
                    isFullyManaged)).thenReturn(messageId);

            sendScheduledPatchingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
            verify(messagingService, times(1)).sendScheduledPatchingEmail(shopperId,
                    accountName, startTime, durationMinutes, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendScheduledPatchingEmailWaitsForMessageComplete()  {
        try {
            when(messagingService.sendScheduledPatchingEmail(shopperId, accountName, startTime, durationMinutes,
                    isFullyManaged)).thenReturn(messageId);

            sendScheduledPatchingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
            verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testSendScheduledPatchingEmailThrowsRuntimeExceptionForIOEx() {
        try {
            IOException testIOException = mock(IOException.class);
            when(messagingService.sendScheduledPatchingEmail(shopperId, accountName, startTime, durationMinutes,
                    isFullyManaged)).thenThrow(testIOException);
            sendScheduledPatchingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }

    @Test(expected = RuntimeException.class)
    public void testSendScheduledPatchingEmailThrowsRuntimeExceptionForMissingShopperEx() {
        try {
            MissingShopperIdException testMissingEx = mock(MissingShopperIdException.class);
            when(messagingService.sendScheduledPatchingEmail(shopperId, accountName, startTime, durationMinutes,
                    isFullyManaged)).thenThrow(testMissingEx);
            sendScheduledPatchingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
        }
        catch (IOException | MissingShopperIdException ex) {
            // Assert fail for unexpected exception
        }
        Assert.fail("Expected RuntimeException");
    }
}
