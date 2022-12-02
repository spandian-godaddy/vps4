package com.godaddy.vps4.orchestration.messaging;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.godaddy.vps4.messaging.models.Message;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.config.Config;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class SendSetupCompletedEmailTest {

    Vps4MessagingService messagingService = mock(Vps4MessagingService.class);
    CreditService creditService = mock(CreditService.class);
    Config config = mock(Config.class);
    SendSetupCompletedEmail command;
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
    VirtualMachineCredit credit = mock(VirtualMachineCredit.class);

    @Before
    public void setupTest() {
        messageId = UUID.randomUUID().toString();
        Message message = mock(Message.class);
        message.status = Message.Statuses.SUCCESS.toString();
        when(config.get("messaging.reseller.blacklist.setup", "")).thenReturn("");
        when(messagingService.sendSetupEmail(shopperId, serverName, ipAddress, orionGuid.toString(), isManaged)).thenReturn(messageId);
        when(messagingService.getMessageById(messageId)).thenReturn(message);
        when(credit.getResellerId()).thenReturn("1");
        when(creditService.getVirtualMachineCredit(anyObject())).thenReturn(credit);

        request = new SetupCompletedEmailRequest(shopperId, isManaged, orionGuid, serverName, ipAddress);
    }

    @Test
    public void testReturnsMessageId() {
        command = new SendSetupCompletedEmail(messagingService, creditService, config);
        String res = command.execute(context, request);
        assertEquals(res,messageId);
    }

    @Test
    public void testCallsMessagingServiceToSendEmail() {
        command = new SendSetupCompletedEmail(messagingService, creditService, config);
        command.execute(context, request);
        verify(messagingService, times(1)).sendSetupEmail(shopperId, serverName, ipAddress,
                orionGuid.toString(), isManaged);
    }
    @Test
    public void testCreditPlidNotInBlacklist() {
        when(config.get("messaging.reseller.blacklist.setup", "")).thenReturn("4500,527397,525848");
        when(credit.getResellerId()).thenReturn("12345");

        command = new SendSetupCompletedEmail(messagingService, creditService, config);
        command.execute(context, request);

        verify(messagingService, times(1)).sendSetupEmail(shopperId, serverName, ipAddress,
                orionGuid.toString(), isManaged);
    }

    @Test(expected=RuntimeException.class)
    public void testCreditPlidInBlacklistThrowsException() {
        when(config.get("messaging.reseller.blacklist.setup", "")).thenReturn("4500,527397,525848");
        when(credit.getResellerId()).thenReturn("4500");

        command = new SendSetupCompletedEmail(messagingService, creditService, config);
        command.execute(context, request);
    }
}
