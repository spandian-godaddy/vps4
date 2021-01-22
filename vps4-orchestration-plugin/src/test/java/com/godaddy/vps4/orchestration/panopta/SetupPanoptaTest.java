package com.godaddy.vps4.orchestration.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanoptaAgent;
import com.godaddy.vps4.orchestration.hfs.sysadmin.UninstallPanoptaAgent;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class SetupPanoptaTest {
    private CommandContext context;
    private CreditService creditService;
    private PanoptaDataService panoptaDataService;
    private PanoptaService panoptaService;
    private Config config;

    private SetupPanopta setupPanopta;
    private SetupPanopta.Request request;

    private final long hfsVmId = 42;
    private final String shopperId = "12345";
    private final String fqdn = "127.0.0.1";
    private final UUID orionGuid = UUID.randomUUID();
    private final UUID vmId = UUID.randomUUID();
    private final String customerKey = "fake-customer-key";
    private final String serverKey = "fake-server-key";
    private final String partnerCustomerKey = "gdtest_" + shopperId;
    private VirtualMachineCredit credit;
    private PanoptaCustomer customer;
    private PanoptaCustomerDetails customerDetails;
    private PanoptaServer server;
    private PanoptaServerDetails serverDetails;

    @Captor private ArgumentCaptor<InstallPanoptaAgent.Request> agentInstallCaptor;
    @Captor private ArgumentCaptor<String[]> templateCaptor;

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        creditService = mock(CreditService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        panoptaService = mock(PanoptaService.class);
        config = mock(Config.class);

        setupCredit();
        setupPanoptaCustomer();
        setupPanoptaServer();
        setupPanoptaAgent();
        setupTemplates();

        setupPanopta = new SetupPanopta(creditService, panoptaDataService, panoptaService, config);
        request = setupRequest();
    }

    private void setupCredit() {
        credit = mock(VirtualMachineCredit.class);
        when(credit.isManaged()).thenReturn(false);
        when(credit.hasMonitoring()).thenReturn(false);
        when(credit.getOperatingSystem()).thenReturn("LINUX");
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
    };

    private void setupPanoptaCustomer() throws PanoptaServiceException {
        customer = new PanoptaCustomer(customerKey, partnerCustomerKey);
        when(panoptaService.createCustomer(shopperId)).thenReturn(customer);
        when(panoptaService.getCustomer(shopperId)).thenReturn(customer);
        customerDetails = mock(PanoptaCustomerDetails.class);
        when(customerDetails.getCustomerKey()).thenReturn(customerKey);
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(customerDetails);
        doNothing().when(panoptaDataService).createPanoptaCustomer(shopperId, customerKey);
    }

    private void setupPanoptaServer() throws PanoptaServiceException {
        server = mock(PanoptaServer.class);
        when(panoptaService.createServer(eq(shopperId), eq(orionGuid), eq(fqdn), any())).thenReturn(server);
        serverDetails = mock(PanoptaServerDetails.class);
        when(serverDetails.getServerKey()).thenReturn(serverKey);
        when(panoptaDataService.getPanoptaServerDetails(vmId)).thenReturn(serverDetails);
        doNothing().when(panoptaDataService).createPanoptaServer(eq(vmId), eq(shopperId), any());
    }

    private void setupPanoptaAgent() {
        when(context.execute(eq(InstallPanoptaAgent.class), any(InstallPanoptaAgent.Request.class))).thenReturn(null);
        when(context.execute(eq(WaitForPanoptaAgentSync.class), any())).thenReturn(null);
        when(context.execute(UninstallPanoptaAgent.class, hfsVmId)).thenReturn(null);
        when(context.execute(RemovePanoptaMonitoring.class, vmId)).thenReturn(null);
    }

    private void setupTemplates() {
        when(config.get("panopta.api.templates.base.linux")).thenReturn("fake_template_base");
        when(config.get("panopta.api.templates.addon.linux")).thenReturn("fake_template_addon");
        when(config.get("panopta.api.templates.managed.linux")).thenReturn("fake_template_managed");
        when(config.get("panopta.api.templates.webhook")).thenReturn("fake_template_dc");
    }

    private SetupPanopta.Request setupRequest() {
        SetupPanopta.Request request = new SetupPanopta.Request();
        request.vmId = vmId;
        request.orionGuid = orionGuid;
        request.hfsVmId = hfsVmId;
        request.shopperId = shopperId;
        request.fqdn = fqdn;
        return request;
    }

    @Test
    public void getsCustomerFromDb() throws PanoptaServiceException {
        setupPanopta.execute(context, request);
        verify(panoptaDataService, times(1)).getPanoptaCustomerDetails(shopperId);
        verify(panoptaService, never()).getCustomer(shopperId);
        verify(panoptaService, never()).createCustomer(shopperId);
        verify(panoptaDataService, never()).createPanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void getsCustomerFromPanoptaAndStoresInDb() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId))
                .thenReturn(null)
                .thenReturn(customerDetails);
        when(panoptaService.getCustomer(shopperId)).thenReturn(null);
        setupPanopta.execute(context, request);
        verify(panoptaDataService, times(2)).getPanoptaCustomerDetails(shopperId);
        verify(panoptaService, times(1)).getCustomer(shopperId);
        verify(panoptaService, times(1)).createCustomer(shopperId);
        verify(panoptaDataService, times(1)).createPanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void createsPanoptaCustomer() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId))
                .thenReturn(null)
                .thenReturn(customerDetails);
        setupPanopta.execute(context, request);
        verify(panoptaDataService, times(2)).getPanoptaCustomerDetails(shopperId);
        verify(panoptaService, times(1)).getCustomer(shopperId);
        verify(panoptaService, never()).createCustomer(shopperId);
        verify(panoptaDataService, times(1)).createPanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void getsServerFromDb() throws PanoptaServiceException {
        setupPanopta.execute(context, request);
        verify(panoptaDataService, times(1)).getPanoptaServerDetails(vmId);
        verify(panoptaService, never()).createServer(any(), any(), any(), any());
    }

    @Test
    public void createsPanoptaServer() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaServerDetails(vmId))
                .thenReturn(null)
                .thenReturn(serverDetails);
        setupPanopta.execute(context, request);
        verify(panoptaDataService, times(2)).getPanoptaServerDetails(vmId);
        verify(panoptaService, times(1))
                .createServer(eq(shopperId), eq(orionGuid), eq(fqdn), templateCaptor.capture());
        String[] templates = templateCaptor.getValue();
        assertEquals("https://api2.panopta.com/v2/server_template/fake_template_base", templates[0]);
        assertEquals("https://api2.panopta.com/v2/server_template/fake_template_dc", templates[1]);
    }

    @Test
    public void createsPanoptaServerWithAddonTemplate() throws PanoptaServiceException {
        when(credit.hasMonitoring()).thenReturn(true);
        when(panoptaDataService.getPanoptaServerDetails(vmId))
                .thenReturn(null)
                .thenReturn(serverDetails);
        setupPanopta.execute(context, request);
        verify(panoptaService, times(1))
                .createServer(eq(shopperId), eq(orionGuid), eq(fqdn), templateCaptor.capture());
        String[] templates = templateCaptor.getValue();
        assertEquals("https://api2.panopta.com/v2/server_template/fake_template_addon", templates[0]);
    }

    @Test
    public void createsPanoptaServerWithManagedTemplate() throws PanoptaServiceException {
        when(credit.isManaged()).thenReturn(true);
        when(panoptaDataService.getPanoptaServerDetails(vmId))
                .thenReturn(null)
                .thenReturn(serverDetails);
        setupPanopta.execute(context, request);
        verify(panoptaService, times(1))
                .createServer(eq(shopperId), eq(orionGuid), eq(fqdn), templateCaptor.capture());
        String[] templates = templateCaptor.getValue();
        assertEquals("https://api2.panopta.com/v2/server_template/fake_template_managed", templates[0]);
    }

    @Test
    public void installsPanoptaAgent() {
        setupPanopta.execute(context, request);
        verify(context, times(1))
                .execute(eq(InstallPanoptaAgent.class), agentInstallCaptor.capture());
        InstallPanoptaAgent.Request request = agentInstallCaptor.getValue();
        assertEquals(customerKey, request.customerKey);
        assertEquals(serverKey, request.serverKey);
        assertEquals(orionGuid.toString(), request.serverName);
        assertEquals("fake_template_base,fake_template_dc", request.templates);
        assertEquals(fqdn, request.fqdn);
        assertEquals(hfsVmId, request.hfsVmId);
    }

    @Test
    public void handlesAgentInstallFailure() {
        try {
            when(context.execute(eq(InstallPanoptaAgent.class), any())).thenThrow(new RuntimeException("test"));
            setupPanopta.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            verify(context, times(1)).execute(eq(InstallPanoptaAgent.class), any());
            verify(context, never()).execute(eq(WaitForPanoptaAgentSync.class), any());
            verify(context, times(1)).execute(UninstallPanoptaAgent.class, hfsVmId);
            verify(context, times(1)).execute(RemovePanoptaMonitoring.class, vmId);
        }
    }

    @Test
    public void handlesAgentInstallAndUninstallFailure() {
        try {
            when(context.execute(eq(InstallPanoptaAgent.class), any())).thenThrow(new RuntimeException("test"));
            when(context.execute(eq(UninstallPanoptaAgent.class), any())).thenThrow(new RuntimeException("test"));
            setupPanopta.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            verify(context, times(1)).execute(eq(InstallPanoptaAgent.class), any());
            verify(context, never()).execute(eq(WaitForPanoptaAgentSync.class), any());
            verify(context, times(1)).execute(UninstallPanoptaAgent.class, hfsVmId);
            verify(context, times(1)).execute(RemovePanoptaMonitoring.class, vmId);
        }
    }

    @Test
    public void waitsForAgentSync() {
        setupPanopta.execute(context, request);
        verify(context, times(1)).execute(eq(WaitForPanoptaAgentSync.class), any());
    }

    @Test
    public void uninstallsIfAgentSyncFails() {
        try {
            when(context.execute(eq(WaitForPanoptaAgentSync.class), any())).thenThrow(new RuntimeException("oof"));
            setupPanopta.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            verify(context, times(1)).execute(eq(InstallPanoptaAgent.class), any());
            verify(context, times(1)).execute(eq(WaitForPanoptaAgentSync.class), any());
            verify(context, times(1)).execute(UninstallPanoptaAgent.class, hfsVmId);
            verify(context, times(1)).execute(RemovePanoptaMonitoring.class, vmId);
        }
    }
}