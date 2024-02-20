package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateManagedPanoptaTemplateTest {
    @Mock private Config config;
    @Mock private CommandContext context;
    @Mock private CreditService creditService;
    @Mock private PanoptaService panoptaService;
    @Mock private VirtualMachineCredit credit;

    @Captor
    private ArgumentCaptor<String> templateCaptor;
    @Captor
    private ArgumentCaptor<String[]> templatesCaptor;

    UpdateManagedPanoptaTemplate command;
    UpdateManagedPanoptaTemplate.Request request;

    @Before
    public void setUp() {
        setUpCredit();
        setUpRequest();
        when(config.get("panopta.api.templates.base.linux")).thenReturn("test-1");
        when(config.get("panopta.api.templates.managed.linux")).thenReturn("test-2");
        command = new UpdateManagedPanoptaTemplate(config, creditService, panoptaService);
    }

    private void setUpCredit() {
        when(credit.getOperatingSystem()).thenReturn("linux");
        when(credit.getEntitlementId()).thenReturn(UUID.randomUUID());
        when(credit.hasMonitoring()).thenReturn(false);
        when(credit.isManaged()).thenReturn(true);
        when(creditService.getVirtualMachineCredit(credit.getEntitlementId())).thenReturn(credit);
    }

    private void setUpRequest() {
        request = new UpdateManagedPanoptaTemplate.Request();
        request.vmId = UUID.randomUUID();
        request.orionGuid = credit.getEntitlementId();
        request.partnerCustomerKey = "test-customer";
        request.serverId = 567856785678L;
    }

    @Test
    public void testRemoveTemplate() {
        command.execute(context, request);
        verify(panoptaService, times(1)).removeTemplate(eq(request.serverId),
                eq(request.partnerCustomerKey),
                templateCaptor.capture(),
                eq("delete"));
        String result = templateCaptor.getValue();
        assertEquals("test-1", result);
    }

    @Test
    public void testApplyTemplate() {
        command.execute(context, request);
        verify(panoptaService, times(1)).applyTemplates(eq(request.serverId),
                eq(request.partnerCustomerKey),
                templatesCaptor.capture());
        String[] result = templatesCaptor.getValue();
        assertEquals(1, result.length);
        assertEquals("https://api2.panopta.com/v2/server_template/test-2", result[0]);
    }
}
