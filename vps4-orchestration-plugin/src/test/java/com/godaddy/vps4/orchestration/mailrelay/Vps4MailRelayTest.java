package com.godaddy.vps4.orchestration.mailrelay;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;

public class Vps4MailRelayTest {

    ActionService actionService = mock(ActionService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);

    Vps4SetMailRelayQuota command = new Vps4SetMailRelayQuota(actionService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testSetMailRelayQuota(){
        MailRelay returnValue = new MailRelay();
        returnValue.quota = 4500;
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class))).thenReturn(returnValue);
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request("1.2.3.4", 4500);
        command.executeWithAction(context, request);
        verify(mailRelayService, times(1)).setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class));
    }

    @Test(expected=RuntimeException.class)
    public void testSetMailRelayQuotaMailRelayReturnsNull(){
        MailRelay returnValue = new MailRelay();
        returnValue.quota = 4500;
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request("1.2.3.4", 4500);
        command.executeWithAction(context, request);
    }

    @Test(expected=RuntimeException.class)
    public void testSetMailRelayQuotaMailReturnsDifferentQuota(){
        MailRelay returnValue = new MailRelay();
        returnValue.quota = 1000;
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class))).thenReturn(returnValue);
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request("1.2.3.4", 4500);
        command.executeWithAction(context, request);
        verify(mailRelayService, times(1)).setRelayQuota(eq("1.2.3.4"), any(MailRelayUpdate.class));
    }

}
