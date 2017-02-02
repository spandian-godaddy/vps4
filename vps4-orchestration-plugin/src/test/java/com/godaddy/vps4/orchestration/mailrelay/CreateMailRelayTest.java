package com.godaddy.vps4.orchestration.mailrelay;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.smtp.CreateMailRelay;
import com.godaddy.vps4.orchestration.hfs.smtp.WaitForMailRelayAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.mailrelay.MailRelayAction;
import gdg.hfs.vhfs.mailrelay.MailRelayAction.Status;
import gdg.hfs.vhfs.mailrelay.MailRelayService;

public class CreateMailRelayTest {

    MailRelayService mailRelayService = mock(MailRelayService.class);
    CreateMailRelay command = new CreateMailRelay(mailRelayService);


    Injector injector = Guice.createInjector(binder -> {
        binder.bind(CreateMailRelay.class);
        binder.bind(WaitForMailRelayAction.class);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testCreateMailRelay() {
        CreateMailRelay.Request createMailRelayRequest = new CreateMailRelay.Request("192.168.1.1");

        MailRelayAction action = new MailRelayAction();
        action.id = 123;
        action.action_id = 321;
        action.status = Status.COMPLETE;
        
        when(mailRelayService.createMailRelay(eq(createMailRelayRequest.ipAddress)))
                .thenReturn(action);
        when(mailRelayService.getMailRelayAction(action.id, action.action_id)).thenReturn(action);

        command.execute(context, createMailRelayRequest);

        verify(mailRelayService, times(1)).createMailRelay(createMailRelayRequest.ipAddress);
    }
}
