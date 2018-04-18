package com.godaddy.vps4.orchestration.messaging;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import gdg.hfs.orchestration.CommandContext;

public class ScheduledMaintenanceEmailRequestTest {
    Vps4MessagingService messagingService;
    CommandContext context;
    SendMessagingEmail sendMessagingEmailCmd;
    String messageId;
    ScheduledMaintenanceEmailRequest scheduledMaintenanceEmailRequest;
    String shopperId;
    String accountName;
    boolean isFullyManaged;
    Instant startTime;
    long durationMinutes;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        sendMessagingEmailCmd = new SendMessagingEmail(messagingService);
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        isFullyManaged = false;
        startTime = Instant.now();
        durationMinutes = Long.MAX_VALUE;

        when(context.execute(WaitForMessageComplete.class, messageId)).thenReturn(null);
    }

    @Test
    public void testSendScheduledPatchingEmail()  {
        try {
            scheduledMaintenanceEmailRequest = new ScheduledMaintenanceEmailRequest(
                    EmailTemplates.VPS4ScheduledPatching, shopperId, accountName, isFullyManaged, startTime,
                    durationMinutes);
            when(messagingService.sendScheduledPatchingEmail(shopperId, accountName, startTime, durationMinutes,
                    isFullyManaged)).thenReturn(messageId);

            sendMessagingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
            verify(messagingService, times(1)).sendScheduledPatchingEmail(shopperId,
                    accountName, startTime, durationMinutes, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendUnexpectedButScheduledMaintenanceEmail()  {
        try {
            scheduledMaintenanceEmailRequest = new ScheduledMaintenanceEmailRequest(
                    EmailTemplates.VPS4UnexpectedbutScheduledMaintenance, shopperId, accountName, isFullyManaged,
                    startTime, durationMinutes);
            when(messagingService.sendUnexpectedButScheduledMaintenanceEmail(shopperId, accountName, startTime,
                    durationMinutes, isFullyManaged)).thenReturn(messageId);

            sendMessagingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
            verify(messagingService, times(1)).sendUnexpectedButScheduledMaintenanceEmail(
                    shopperId, accountName, startTime, durationMinutes, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = UnknownEmailTemplateException.class)
    public void testUnknownEmailTemplateException()  {
        scheduledMaintenanceEmailRequest = new ScheduledMaintenanceEmailRequest(
                EmailTemplates.VPS4SystemDownFailover, shopperId, accountName, isFullyManaged,
                startTime, durationMinutes);

        sendMessagingEmailCmd.execute(context, scheduledMaintenanceEmailRequest);
        Assert.fail("Expected UnknownEmailTemplateException");
    }
}
