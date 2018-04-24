package com.godaddy.vps4.orchestration.messaging;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import gdg.hfs.orchestration.CommandContext;

public class FailOverEmailRequestTest {
    Vps4MessagingService messagingService;
    CommandContext context;
    SendMessagingEmail sendMessagingEmailCmd;
    String messageId;
    FailOverEmailRequest failOverEmailRequest;
    String shopperId;
    String accountName;
    boolean isFullyManaged;

    @Before
    public void setUp() {
        messageId = UUID.randomUUID().toString();
        messagingService = mock(Vps4MessagingService.class);
        context = mock(CommandContext.class);
        sendMessagingEmailCmd = new SendMessagingEmail(messagingService);
        shopperId = UUID.randomUUID().toString();
        accountName = UUID.randomUUID().toString();
        isFullyManaged = false;

        when(context.execute(WaitForMessageComplete.class, messageId)).thenReturn(null);
    }

    @Test
    public void testSendSystemDownFailoverEmail()  {
        try {
            failOverEmailRequest = new FailOverEmailRequest(EmailTemplates.VPS4SystemDownFailoverV2, shopperId,
                    accountName, isFullyManaged);
            when(messagingService.sendSystemDownFailoverEmail(shopperId, accountName, isFullyManaged))
                    .thenReturn(messageId);

            sendMessagingEmailCmd.execute(context, failOverEmailRequest);
            verify(messagingService, times(1)).sendSystemDownFailoverEmail(shopperId,
                    accountName, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void testSendFailoverCompletedEmail()  {
        try {
            failOverEmailRequest = new FailOverEmailRequest(EmailTemplates.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted,
                    shopperId, accountName, isFullyManaged);
            when(messagingService.sendFailoverCompletedEmail(shopperId, accountName, isFullyManaged))
                    .thenReturn(messageId);

            sendMessagingEmailCmd.execute(context, failOverEmailRequest);
            verify(messagingService, times(1)).sendFailoverCompletedEmail(shopperId,
                    accountName, isFullyManaged);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test(expected = UnknownEmailTemplateException.class)
    public void testUnknownEmailTemplateException()  {
        failOverEmailRequest = new FailOverEmailRequest(EmailTemplates.VPS4ScheduledPatchingV2,
                shopperId, accountName, isFullyManaged);

        sendMessagingEmailCmd.execute(context, failOverEmailRequest);
        Assert.fail("Expected UnknownEmailTemplateException");
    }
}
