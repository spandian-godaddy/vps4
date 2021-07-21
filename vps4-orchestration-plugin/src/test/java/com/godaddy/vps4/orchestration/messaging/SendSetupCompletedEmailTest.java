package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class SendSetupCompletedEmailTest {

    Vps4MessagingService messagingService = mock(Vps4MessagingService.class);
    SendSetupCompletedEmail command = new SendSetupCompletedEmail(messagingService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4MessagingService.class).toInstance(messagingService);
    });

    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    SetupCompletedEmailRequest request;
    String messageId;
    String shopperId= "123456";
    String serverName= "MyNewServer";
    String ipAddress= "127.0.0.1";
    boolean isManaged = true;
    UUID orionGuid = UUID.randomUUID();

    @Before
    public void setupTest() {
        request = new SetupCompletedEmailRequest(shopperId, isManaged, orionGuid, serverName, ipAddress);

        messageId = UUID.randomUUID().toString();
        Message message = mock(Message.class);
        message.status = Message.Statuses.SUCCESS.toString();

        when(messagingService.sendSetupEmail(shopperId, serverName, ipAddress, orionGuid.toString(), isManaged)).thenReturn(messageId);
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
        verify(messagingService, times(1)).sendSetupEmail(shopperId, serverName, ipAddress,
                orionGuid.toString(), isManaged);
    }

    @Test
    public void testWaitsForMessageComplete() {
        command.execute(context, request);
        verify(context, times(1)).execute(WaitForMessageComplete.class, messageId);
    }
}
