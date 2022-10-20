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

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class SendUnexpectedButScheduledMaintenanceEmailTest {

    Vps4MessagingService messagingService = mock(Vps4MessagingService.class);
    SendUnexpectedButScheduledMaintenanceEmail command = new SendUnexpectedButScheduledMaintenanceEmail(messagingService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4MessagingService.class).toInstance(messagingService);
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
        Message message = mock(Message.class);
        message.status = Message.Statuses.SUCCESS.toString();

        when(messagingService.sendUnexpectedButScheduledMaintenanceEmail("shopperid", "vmname", startTime, 30, false)).thenReturn(messageId);
        when(messagingService.getMessageById(messageId)).thenReturn(message);
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

    @Test
    public void testWaitsForMessageComplete() {
        command.execute(context, request);
        verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
    }
}

