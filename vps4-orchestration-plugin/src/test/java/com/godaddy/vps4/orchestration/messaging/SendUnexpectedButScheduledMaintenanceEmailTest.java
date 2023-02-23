package com.godaddy.vps4.orchestration.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.messaging.MessagingService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class SendUnexpectedButScheduledMaintenanceEmailTest {

    MessagingService messagingService = mock(MessagingService.class);
    SendUnexpectedButScheduledMaintenanceEmail command = new SendUnexpectedButScheduledMaintenanceEmail(messagingService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(MessagingService.class).toInstance(messagingService);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    ScheduledMaintenanceEmailRequest request;
    Instant startTime = Instant.now();
    String messageId;

    @Before
    public void setupTest() {
        request = new ScheduledMaintenanceEmailRequest();
        request.accountName = "vmname";
        request.shopperId = "shopperid";
        request.isManaged = false;
        request.startTime = startTime;
        request.durationMinutes = 30;

        messageId = UUID.randomUUID().toString();

        when(messagingService.sendUnexpectedButScheduledMaintenanceEmail("shopperid", "vmname", startTime, 30, false)).thenReturn(messageId);
    }

    @Test
    public void testReturnsMessageId() {
        String res = command.execute(context, request);
        assertEquals(res,messageId);
    }

    @Test
    public void testCallsMessagingServiceToSendEmail() {
        command.execute(context, request);
        verify(messagingService, times(1)).sendUnexpectedButScheduledMaintenanceEmail("shopperid", "vmname", startTime, 30, false);
    }
}
