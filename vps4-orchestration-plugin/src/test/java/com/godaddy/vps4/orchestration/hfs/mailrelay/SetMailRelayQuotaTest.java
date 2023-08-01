package com.godaddy.vps4.orchestration.hfs.mailrelay;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.plesk.SetPleskOutgoingEmailIp;
import com.godaddy.vps4.orchestration.hfs.plesk.WaitForPleskAction;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import static org.junit.Assert.assertEquals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SetMailRelayQuotaTest {

    MailRelayService mailRelayService;
    SetMailRelayQuota command;
    CommandContext context;
    Injector injector;

    SetMailRelayQuota.Request request;
    MailRelay mailRelay;

    @Before
    public void setup() throws Exception {
        mailRelay = new MailRelay();
        mailRelayService = mock(MailRelayService.class);
        command = new SetMailRelayQuota(mailRelayService);
        request = new SetMailRelayQuota.Request();
        request.quota = 150;
        request.relays = 5;
        request.isAdditionalIp = false;
        request.ipAddress = "1.2.3.4";

        mailRelay.quota = request.quota;
        mailRelay.relays = request.relays;
        mailRelay.ipv4Address = request.ipAddress;
        when(mailRelayService.setRelayQuota(any(), any())).thenReturn(mailRelay);
        when(mailRelayService.getMailRelay(any())).thenReturn(mailRelay);

        injector = Guice.createInjector(binder -> {
            binder.bind(SetMailRelayQuota.class);
            binder.bind(MailRelayService.class).toInstance(mailRelayService);
        });
        context = new TestCommandContext(new GuiceCommandProvider(injector));

    }

    @After
    public void tearDown() {
        mailRelayService = null;
        command = null;
        injector = null;
        context = null;
    }

    @Test
    public void testExecuteSuccess() {
        ArgumentCaptor<MailRelayUpdate> captor = ArgumentCaptor.forClass(MailRelayUpdate.class);

        command.execute(context, request);

        verify(mailRelayService, times(0)).getMailRelay(request.ipAddress);
        verify(mailRelayService, times(1)).setRelayQuota(eq(request.ipAddress), captor.capture());
        MailRelayUpdate updateReq = captor.getValue();
        assertEquals(request.quota, updateReq.quota);
        assertEquals(request.relays, updateReq.relays);
    }

    @Test(expected = RuntimeException.class)
    public void testReceivedNullMailRelay() {
        when(mailRelayService.setRelayQuota(any(), any())).thenReturn(null);

        command.execute(context, request);

        verify(mailRelayService, times(0)).getMailRelay(request.ipAddress);
        verify(mailRelayService, times(1)).setRelayQuota(eq(request.ipAddress), any(MailRelayUpdate.class));
    }

    @Test(expected = RuntimeException.class)
    public void testReceivedDifferentRelay() {
        mailRelay.relays = request.relays + 5;

        command.execute(context, request);

        verify(mailRelayService, times(0)).getMailRelay(request.ipAddress);
        verify(mailRelayService, times(1)).setRelayQuota(eq(request.ipAddress), any(MailRelayUpdate.class));
    }

    @Test(expected = RuntimeException.class)
    public void testReceivedDifferentQuota() {
        mailRelay.quota = request.quota + 5;

        command.execute(context, request);

        verify(mailRelayService, times(0)).getMailRelay(request.ipAddress);
        verify(mailRelayService, times(1)).setRelayQuota(eq(request.ipAddress), any(MailRelayUpdate.class));
    }

    @Test
    public void testZeroQuotaCallsGetQuotaForPrimaryIp() {
        ArgumentCaptor<MailRelayUpdate> captor = ArgumentCaptor.forClass(MailRelayUpdate.class);

        request.quota = 0;
        mailRelay.quota = 50;

        command.execute(context, request);

        verify(mailRelayService, times(1)).getMailRelay(request.ipAddress);
        verify(mailRelayService, times(1)).setRelayQuota(eq(request.ipAddress), captor.capture());

        MailRelayUpdate updateReq = captor.getValue();
        assertEquals(mailRelay.quota, updateReq.quota);
        assertEquals(request.relays, updateReq.relays);
    }

    @Test
    public void testZeroQuotaDoesNOTCallGetQuotaForAdditionalIp() {
        ArgumentCaptor<MailRelayUpdate> captor = ArgumentCaptor.forClass(MailRelayUpdate.class);

        request.quota = 0;
        request.isAdditionalIp = true;

        mailRelay.quota = request.quota;

        command.execute(context, request);

        verify(mailRelayService, times(0)).getMailRelay(request.ipAddress);
        verify(mailRelayService, times(1)).setRelayQuota(eq(request.ipAddress), captor.capture());

        MailRelayUpdate updateReq = captor.getValue();
        assertEquals(request.quota, updateReq.quota);
        assertEquals(request.relays, updateReq.relays);

    }
}
