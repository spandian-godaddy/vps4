package com.godaddy.vps4.orchestration.mailrelay;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4MailRelayTest {

    ActionService actionService = mock(ActionService.class);
    MailRelayService mailRelayService = mock(MailRelayService.class);

    Vps4SetMailRelayQuota setQuotaCommand = new Vps4SetMailRelayQuota(actionService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(MailRelayService.class).toInstance(mailRelayService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void testSetMailRelayQuota(){
        int quota = 4500;
        MailRelay returnValue = new MailRelay();
        returnValue.quota = quota;
        ArgumentCaptor<MailRelayUpdate> argCaptor = ArgumentCaptor.forClass(MailRelayUpdate.class);
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), argCaptor.capture())).thenReturn(returnValue);
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request("1.2.3.4", quota);

        setQuotaCommand.executeWithAction(context, request);

        verify(mailRelayService, times(1)).setRelayQuota("1.2.3.4", argCaptor.getValue());
        Assert.assertEquals(quota, argCaptor.getValue().quota);
    }

    @Test(expected=RuntimeException.class)
    public void testSetMailRelayQuotaMailRelayReturnsNull(){
        MailRelay returnValue = new MailRelay();
        returnValue.quota = 4500;
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request("1.2.3.4", 4500);

        setQuotaCommand.executeWithAction(context, request);
    }

    @Test(expected=RuntimeException.class)
    public void testSetMailRelayQuotaMailReturnsDifferentQuota(){
        MailRelay returnValue = new MailRelay();
        returnValue.quota = 1000;
        ArgumentCaptor<MailRelayUpdate> argCaptor = ArgumentCaptor.forClass(MailRelayUpdate.class);
        when(mailRelayService.setRelayQuota(eq("1.2.3.4"), argCaptor.capture())).thenReturn(returnValue);
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request("1.2.3.4", 4500);

        setQuotaCommand.executeWithAction(context, request);

        verify(mailRelayService, times(1)).setRelayQuota("1.2.3.4", argCaptor.getValue());
    }
}
