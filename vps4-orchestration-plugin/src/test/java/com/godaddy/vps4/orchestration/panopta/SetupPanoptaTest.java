package com.godaddy.vps4.orchestration.panopta;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
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
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanopta;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;

@RunWith(MockitoJUnitRunner.class)
public class SetupPanoptaTest {

    private CreditService creditServiceMock;
    private CommandContext commandContextMock;
    private PanoptaServer panoptaServerMock;
    private PanoptaCustomer panoptaCustomerMock;
    private PanoptaCustomerDetails panoptaCustomerDetailsMock;
    private PanoptaServerDetails panoptaServerDetailsMock;
    private CreatePanoptaCustomer.Response createPanoptaCustomerResponse;
    private PanoptaDataService panoptaDataServiceMock;
    private PanoptaService panoptaServiceMock;
    private Config configMock;
    private SetupPanopta command;
    private SetupPanopta.Request request;
    private SysAdminAction sysAdminActionMock;
    private UUID fakeVmId;
    private UUID fakeOrionGuid;
    private long fakeHfsVmId;
    private String fakeCustomerKey;
    private String fakePanoptaTemplates;
    private String fakeServerKey;
    private String fakeServerName;
    private String fakeShopperId;
    private String fakeDataCenterTemplate;
    private VirtualMachineCredit creditMock;
    @Captor
    private ArgumentCaptor<CreatePanoptaCustomer.Request> createPanoptaRequestCaptor;
    @Captor
    private ArgumentCaptor<InstallPanopta.Request> installPanoptaRequestCaptor;
    @Captor
    private ArgumentCaptor<WaitForPanoptaInstall.Request> getPanoptaServerRequestCaptor;

    @Before
    public void setUp() throws Exception {
        fakeVmId = UUID.randomUUID();
        fakeOrionGuid = UUID.randomUUID();
        fakeHfsVmId = 1234L;
        fakeShopperId = "fake-shopper-id";
        fakeCustomerKey = "so-very-fake-customer-key";
        fakeServerKey = "ultra-fake-server-key";
        fakeServerName = fakeOrionGuid.toString();
        fakePanoptaTemplates = "super-fake-panopta-template";
        fakeDataCenterTemplate = "mega-fake-data-center-template";
        creditServiceMock = mock(CreditService.class);
        commandContextMock = mock(CommandContext.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        panoptaServerMock = mock(PanoptaServer.class);
        panoptaDataServiceMock = mock(PanoptaDataService.class);
        panoptaServiceMock = mock(PanoptaService.class);
        configMock = mock(Config.class);
        creditMock = mock(VirtualMachineCredit.class);
        sysAdminActionMock = mock(SysAdminAction.class);
        panoptaCustomerDetailsMock = mock(PanoptaCustomerDetails.class);
        panoptaServerDetailsMock = mock(PanoptaServerDetails.class);

        command = new SetupPanopta(creditServiceMock, panoptaDataServiceMock, panoptaServiceMock, configMock);

        setupFakePanoptaCustomerResponse();
        setupMockContext();
        setupCommandRequest();
        setupMocksForTests();
    }

    private void setupMockContext() {
        when(commandContextMock.execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class)))
                .thenReturn(createPanoptaCustomerResponse);
        when(commandContextMock.execute(eq(GetPanoptaServerKeyFromHfs.class), anyLong()))
                .thenReturn(sysAdminActionMock);
        when(commandContextMock.execute(eq(WaitForPanoptaInstall.class), any(WaitForPanoptaInstall.Request.class)))
                .thenReturn(panoptaServerMock);
    }

    private void setupCommandRequest() {
        request = new SetupPanopta.Request();
        request.vmId = fakeVmId;
        request.orionGuid = fakeOrionGuid;
        request.hfsVmId = fakeHfsVmId;
        request.shopperId = fakeShopperId;
    }

    private void setupFakePanoptaCustomerResponse() {
        createPanoptaCustomerResponse = new CreatePanoptaCustomer.Response();
        createPanoptaCustomerResponse.panoptaCustomer = panoptaCustomerMock;
    }

    private void setupMocksForTests() {
        when(creditServiceMock.getVirtualMachineCredit(eq(fakeOrionGuid))).thenReturn(creditMock);
        when(creditMock.getOperatingSystem()).thenReturn("linux");
        when(creditMock.isManaged()).thenReturn(true);
        when(configMock.get(eq("panopta.api.templates.managed.linux"))).thenReturn(fakePanoptaTemplates);
        when(configMock.get("panopta.api.templates.webhook")).thenReturn(fakeDataCenterTemplate);
        when(panoptaServiceMock.getActiveServers(fakeShopperId)).thenReturn(new ArrayList<>());
        when(panoptaServiceMock.getSuspendedServers(fakeShopperId)).thenReturn(new ArrayList<>());
    }

    @Test
    public void invokesCreatePanoptaCustomer() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId))).thenReturn(null).thenReturn(
                panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(null);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class));
        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), createPanoptaRequestCaptor.capture());
        CreatePanoptaCustomer.Request capturedRequest = createPanoptaRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", fakeVmId, capturedRequest.vmId);
    }

    @Test
    public void invokesWaitForPanoptaInstall() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(WaitForPanoptaInstall.class), getPanoptaServerRequestCaptor.capture());
        WaitForPanoptaInstall.Request capturedRequest = getPanoptaServerRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", fakeVmId, capturedRequest.vmId);
        assertEquals("Expected shopper id in request does not match actual value. ", fakeShopperId,
                     capturedRequest.shopperId);
    }

    @Test
    public void invokesGetServerKeyFromHfs() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(GetPanoptaServerKeyFromHfs.class), eq(fakeHfsVmId));
    }

    @Test
    public void invokesInstallPanoptaOnProvisions() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals("Expected HFS vm id in request does not match actual value. ", fakeHfsVmId,
                     capturedRequest.hfsVmId);
        assertEquals(fakeCustomerKey, capturedRequest.customerKey);
        assertEquals(fakePanoptaTemplates + "," + fakeDataCenterTemplate, capturedRequest.templates);
    }

    @Test
    public void installPanoptaOnProvisionsRemovesOrphans() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);

        PanoptaServer orphan = mock(PanoptaServer.class);
        orphan.serverId = 15;
        orphan.name = fakeOrionGuid.toString();
        when(panoptaServiceMock.getActiveServers(fakeShopperId)).thenReturn(Collections.singletonList(orphan));

        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        verify(panoptaServiceMock, times(1)).removeServerMonitoring(orphan.serverId, fakeShopperId);
    }

    @Test
    public void installPanoptaOnProvisionCompletesEvenIfPanoptaFails() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        when(commandContextMock.execute(eq(InstallPanopta.class), any()))
                .thenThrow(new RuntimeException("something broke"));
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), any());
        verify(panoptaDataServiceMock, never()).createPanoptaServer(any(), any(), any());
    }

    @Test
    public void invokesInstallPanoptaOnRebuilds() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(panoptaServerDetailsMock);
        when(panoptaServerDetailsMock.getServerKey()).thenReturn(fakeServerKey);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals("Expected HFS vm id in request does not match actual value. ", capturedRequest.hfsVmId,
                     fakeHfsVmId);
        assertEquals(fakeCustomerKey, capturedRequest.customerKey);
        assertEquals(fakeServerKey, capturedRequest.serverKey);
        assertEquals(fakePanoptaTemplates + "," + fakeDataCenterTemplate, capturedRequest.templates);
    }

    @Test
    public void installPanoptaOnRebuildFailsIfPanoptaFails() {
        try {
            when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                    .thenReturn(panoptaCustomerDetailsMock);
            when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(panoptaServerDetailsMock);
            when(panoptaServerDetailsMock.getServerKey()).thenReturn(fakeServerKey);
            when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
            when(commandContextMock.execute(eq(InstallPanopta.class), any()))
                    .thenThrow(new RuntimeException("something broke"));
            command.execute(commandContextMock, request);
            fail();
        } catch (RuntimeException e) {
            verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), any());
        }
    }

    @Test
    public void installsBasePlusTemplateForUnmanagedWithMonitoring() {
        String fakeUnmanagedServerTemplate = "fake-unmanaged-panopta-template";
        when(creditMock.isManaged()).thenReturn(false);
        when(creditMock.hasMonitoring()).thenReturn(true);
        when(configMock.get(eq("panopta.api.templates.addon.linux"))).thenReturn(fakeUnmanagedServerTemplate);

        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(panoptaServerDetailsMock);
        when(panoptaServerDetailsMock.getServerKey()).thenReturn(fakeServerKey);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals("Expected HFS vm id in request does not match actual value. ", capturedRequest.hfsVmId,
                     fakeHfsVmId);
        assertEquals(fakeCustomerKey, capturedRequest.customerKey);
        assertEquals(fakeServerKey, capturedRequest.serverKey);
        assertEquals(fakeUnmanagedServerTemplate + "," + fakeDataCenterTemplate, capturedRequest.templates);
        assertThat(capturedRequest.templates, not(containsString(fakePanoptaTemplates)));
    }

    @Test
    public void checksIfCustomerExistsInPanoptaBeforeCreate() {
        when(commandContextMock.execute(eq(GetPanoptaCustomer.class), eq(fakeShopperId))).thenReturn(null);
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId))).thenReturn(null).thenReturn(
                panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(null);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock).execute(eq(GetPanoptaCustomer.class), eq(fakeShopperId));
        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class));
        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), createPanoptaRequestCaptor.capture());
        CreatePanoptaCustomer.Request capturedRequest = createPanoptaRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", fakeVmId, capturedRequest.vmId);
    }

    @Test
    public void doesNotCreateCustomerInPanoptaIfOneExists() {
        when(commandContextMock.execute(GetPanoptaCustomer.class, fakeShopperId)).thenReturn(panoptaCustomerMock);
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId))).thenReturn(null).thenReturn(
                panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(null);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock).execute(eq(GetPanoptaCustomer.class), eq(fakeShopperId));
        verify(commandContextMock, never())
                .execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class));
    }

    @Test
    public void setServerNameAsOrionGUID() {
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId)))
                .thenReturn(panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(panoptaServerDetailsMock);
        when(panoptaServerDetailsMock.getServerKey()).thenReturn(fakeServerKey);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);
        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals(fakeOrionGuid.toString(), capturedRequest.serverName);
    }
}