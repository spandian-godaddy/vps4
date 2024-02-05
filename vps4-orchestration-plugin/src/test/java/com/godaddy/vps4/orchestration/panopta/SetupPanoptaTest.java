package com.godaddy.vps4.orchestration.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import java.util.Arrays;
import java.util.Map;
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
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanoptaAgent;
import com.godaddy.vps4.orchestration.hfs.sysadmin.UninstallPanoptaAgent;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.reseller.ResellerService;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class SetupPanoptaTest {
    @Mock private CommandContext context;
    @Mock private CreditService creditService;
    @Mock private PanoptaDataService panoptaDataService;
    @Mock private PanoptaService panoptaService;
    @Mock private ResellerService resellerService;
    @Mock private Config config;

    private SetupPanopta setupPanopta;
    private SetupPanopta.Request request;

    private final long hfsVmId = 42;
    private final String shopperId = "12345";
    private final String fqdn = "127.0.0.1";
    private final UUID orionGuid = UUID.randomUUID();
    private final UUID vmId = UUID.randomUUID();
    private final String customerKey = "fake-customer-key";
    private final long serverId = 9876;
    private final String serverKey = "fake-server-key";
    private final String partnerCustomerKey = "gdtest_" + shopperId;
    private VirtualMachineCredit credit;
    private PanoptaCustomerDetails customerDetails;
    private PanoptaServerDetails serverDetails;

    @Captor private ArgumentCaptor<ApplyPanoptaTemplates.Request> applyTemplatesCaptor;
    @Captor private ArgumentCaptor<InstallPanoptaAgent.Request> agentInstallCaptor;
    @Captor private ArgumentCaptor<RemovePanoptaMonitoring.Request> removeMonitoringCaptor;
    @Captor private ArgumentCaptor<String[]> tagCaptor;
    @Captor private ArgumentCaptor<Map<Long, String>> attributeCaptor;

    @Before
    public void setUp() throws Exception {
        setupCredit();
        setupPanoptaCustomer();
        setupPanoptaServer();
        setupPanoptaAgent();
        setupConfig();

        setupPanopta = new SetupPanopta(creditService, panoptaDataService, panoptaService, resellerService, config);
        request = setupRequest();
    }

    private void setupCredit() {
        credit = mock(VirtualMachineCredit.class);
        when(credit.getOperatingSystem()).thenReturn("LINUX");
        when(credit.getEntitlementId()).thenReturn(orionGuid);
        when(credit.getResellerId()).thenReturn("1");
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
    }

    private void setupPanoptaCustomer() throws PanoptaServiceException {
        customerDetails = mock(PanoptaCustomerDetails.class);
        when(customerDetails.getCustomerKey()).thenReturn(customerKey);
        when(panoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId))
                .thenReturn(customerDetails);
    }

    private void setupPanoptaServer() throws PanoptaServiceException {
        PanoptaServer server = mock(PanoptaServer.class);
        when(panoptaService.createServer(eq(shopperId), eq(orionGuid), eq(fqdn), any())).thenReturn(server);
        when(panoptaService.getServer(eq(vmId))).thenReturn(server);
        serverDetails = mock(PanoptaServerDetails.class);
        serverDetails.setServerId(0L);
        serverDetails.setPartnerCustomerKey(partnerCustomerKey);
        when(serverDetails.getPartnerCustomerKey()).thenReturn(partnerCustomerKey);
        when(serverDetails.getServerId()).thenReturn(serverId);
        when(serverDetails.getServerKey()).thenReturn(serverKey);
        when(panoptaDataService.getPanoptaServerDetails(vmId)).thenReturn(serverDetails);
        doNothing().when(panoptaDataService).createPanoptaServer(eq(vmId), eq(shopperId), any(), any());
    }

    private void setupPanoptaAgent() {
        when(context.execute(eq(InstallPanoptaAgent.class), any(InstallPanoptaAgent.Request.class))).thenReturn(null);
        when(context.execute(eq(WaitForPanoptaAgentSync.class), any())).thenReturn(null);
        when(context.execute(UninstallPanoptaAgent.class, hfsVmId)).thenReturn(null);
        when(context.execute(eq(RemovePanoptaMonitoring.class), any(RemovePanoptaMonitoring.Request.class))).thenReturn(null);
    }

    private void setupConfig() {
        when(config.get("panopta.api.templates.base.linux")).thenReturn("fake_template_base");
        when(config.get("panopta.api.templates.addon.linux")).thenReturn("fake_template_addon");
        when(config.get("panopta.api.templates.managed.linux")).thenReturn("fake_template_managed");
        when(config.get("panopta.api.templates.webhook")).thenReturn("fake_template_dc");
        when(config.get("panopta.api.attribute.plid")).thenReturn("4410");
        when(config.get("panopta.api.attribute.brand")).thenReturn("721");
        when(config.get("panopta.api.attribute.product")).thenReturn("722");
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
    public void callsValidateAndGetOrCreatePanoptaCustomer() throws PanoptaServiceException {
        when(panoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId))
                .thenReturn(customerDetails);
        setupPanopta.execute(context, request);
        verify(panoptaService, times(1)).validateAndGetOrCreatePanoptaCustomer(shopperId);
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
        when(panoptaService.getServer(vmId))
                .thenReturn(null);
        setupPanopta.execute(context, request);
        verify(panoptaDataService, times(2)).getPanoptaServerDetails(vmId);
        verify(panoptaService, times(1))
                .createServer(eq(shopperId), eq(orionGuid), eq(fqdn), tagCaptor.capture());

        String[] tags = tagCaptor.getValue();
        assertTrue(Arrays.asList(tags).contains("1"));
        assertTrue(Arrays.asList(tags).contains("godaddy"));
        assertTrue(Arrays.asList(tags).contains("vps4"));
        verify(panoptaDataService, times(1)).createPanoptaServer(eq(vmId), eq(shopperId), eq("fake_template_base"), any());

        verify(panoptaService, times(1)).setServerAttributes(eq(vmId), attributeCaptor.capture());
        Map<Long, String> attributes = attributeCaptor.getValue();
        assertEquals("1", attributes.get(4410L));
        assertEquals("godaddy", attributes.get(721L));
        assertEquals("vps4", attributes.get(722L));
    }

    @Test
    public void createsPanoptaServerWithTemplates() {
        when(panoptaDataService.getPanoptaServerDetails(vmId)).thenReturn(null).thenReturn(serverDetails);
        when(panoptaService.getServer(vmId)).thenReturn(null);
        setupPanopta.execute(context, request);

        verify(context, times(1)).execute(eq(ApplyPanoptaTemplates.class), applyTemplatesCaptor.capture());

        ApplyPanoptaTemplates.Request r = applyTemplatesCaptor.getValue();
        assertEquals(vmId, r.vmId);
        assertEquals(orionGuid, r.orionGuid);
        assertEquals(partnerCustomerKey, r.partnerCustomerKey);
        assertEquals(serverId, r.serverId);
    }

    @Test
    public void createsPanoptaServerAsReseller() throws PanoptaServiceException {
        when(credit.getResellerId()).thenReturn("495469");
        when(resellerService.getResellerDescription(anyString())).thenReturn("media-temple");
        when(panoptaDataService.getPanoptaServerDetails(vmId))
                .thenReturn(null)
                .thenReturn(serverDetails);
        when(panoptaService.getServer(vmId))
                .thenReturn(null);

        setupPanopta.execute(context, request);

        verify(panoptaService, times(1))
                .createServer(eq(shopperId), eq(orionGuid), eq(fqdn), tagCaptor.capture());
        String[] tags = tagCaptor.getValue();
        assertFalse(Arrays.asList(tags).contains("1"));
        assertTrue(Arrays.asList(tags).contains("495469"));
        assertFalse(Arrays.asList(tags).contains("godaddy"));
        assertTrue(Arrays.asList(tags).contains("media-temple"));

        verify(panoptaService, times(1)).setServerAttributes(eq(vmId), attributeCaptor.capture());
        Map<Long, String> attributes = attributeCaptor.getValue();
        assertEquals("495469", attributes.get(4410L));
        assertEquals("media-temple", attributes.get(721L));
        assertEquals("vps4", attributes.get(722L));
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
        assertEquals("fake_template_base,fake_template_dc", request.templateIds);
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
            verify(context, times(1)).execute(eq(RemovePanoptaMonitoring.class), any());
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
            verify(context, times(1)).execute(eq(RemovePanoptaMonitoring.class), removeMonitoringCaptor.capture());

            RemovePanoptaMonitoring.Request r = removeMonitoringCaptor.getValue();
            assertEquals(vmId, r.vmId);
            assertEquals(orionGuid, r.orionGuid);
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
            verify(context, times(1)).execute(eq(RemovePanoptaMonitoring.class), removeMonitoringCaptor.capture());

            RemovePanoptaMonitoring.Request r = removeMonitoringCaptor.getValue();
            assertEquals(vmId, r.vmId);
            assertEquals(orionGuid, r.orionGuid);
        }
    }
}