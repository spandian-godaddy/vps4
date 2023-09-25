package com.godaddy.vps4.orchestration.panopta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class ApplyPanoptaTemplatesTest {
    @Mock private Config config;
    @Mock private CommandContext context;
    @Mock private CreditService creditService;
    @Mock private PanoptaService panoptaService;

    @Captor private ArgumentCaptor<String[]> templatesCaptor;

    @Mock private VirtualMachineCredit credit;

    ApplyPanoptaTemplates command;
    ApplyPanoptaTemplates.Request request;

    @Before
    public void setUp() {
        setUpCredit();
        setUpRequest();
        when(config.get("panopta.api.templates.base.linux")).thenReturn("test-1");
        when(config.get("panopta.api.templates.webhook")).thenReturn("test-2");
        command = new ApplyPanoptaTemplates(config, creditService, panoptaService);
    }

    private void setUpCredit() {
        when(credit.getOperatingSystem()).thenReturn("linux");
        when(credit.getOrionGuid()).thenReturn(UUID.randomUUID());
        when(credit.hasMonitoring()).thenReturn(false);
        when(credit.isManaged()).thenReturn(false);
        when(creditService.getVirtualMachineCredit(credit.getOrionGuid())).thenReturn(credit);
    }

    private void setUpRequest() {
        request = new ApplyPanoptaTemplates.Request();
        request.vmId = UUID.randomUUID();
        request.orionGuid = credit.getOrionGuid();
        request.partnerCustomerKey = "test-customer";
        request.serverId = 567856785678L;
    }

    @Test
    public void testApplyTemplates() throws PanoptaServiceException {
        command.execute(context, request);
        verify(panoptaService, times(1)).applyTemplates(eq(request.serverId),
                                                        eq(request.partnerCustomerKey),
                                                        templatesCaptor.capture());
        String[] result = templatesCaptor.getValue();
        assertEquals(2, result.length);
        assertEquals("https://api2.panopta.com/v2/server_template/test-1", result[0]);
        assertEquals("https://api2.panopta.com/v2/server_template/test-2", result[1]);
    }
}
