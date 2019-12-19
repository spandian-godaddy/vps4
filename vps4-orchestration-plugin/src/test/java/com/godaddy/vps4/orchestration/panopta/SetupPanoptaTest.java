package com.godaddy.vps4.orchestration.panopta;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
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
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanopta;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
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
    private GetPanoptaCustomer.Response getPanoptaCustomerResponse;
    private CreatePanoptaCustomer.Response createPanoptaCustomerResponse;
    private PanoptaDataService panoptaDataServiceMock;
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
        fakePanoptaTemplates = "super-fake-panopta-template";
        fakeDataCenterTemplate = "mega-fake-data-center-template";
        creditServiceMock = mock(CreditService.class);
        commandContextMock = mock(CommandContext.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        panoptaServerMock = mock(PanoptaServer.class);
        panoptaDataServiceMock = mock(PanoptaDataService.class);
        configMock = mock(Config.class);
        creditMock = mock(VirtualMachineCredit.class);
        sysAdminActionMock = mock(SysAdminAction.class);
        panoptaCustomerDetailsMock = mock(PanoptaCustomerDetails.class);
        panoptaServerDetailsMock = mock(PanoptaServerDetails.class);

        command = new SetupPanopta(creditServiceMock, panoptaDataServiceMock, configMock);

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
        getPanoptaCustomerResponse = new GetPanoptaCustomer.Response();
        getPanoptaCustomerResponse.panoptaCustomer = panoptaCustomerMock;
        when(commandContextMock.execute(eq(GetPanoptaCustomer.class), eq(fakeShopperId)))
                .thenReturn(getPanoptaCustomerResponse);
        when(panoptaDataServiceMock.getPanoptaCustomerDetails(eq(fakeShopperId))).thenReturn(null).thenReturn(
                panoptaCustomerDetailsMock);
        when(panoptaDataServiceMock.getPanoptaServerDetails(eq(fakeVmId))).thenReturn(null);
        when(panoptaCustomerDetailsMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock).execute(eq(GetPanoptaCustomer.class), eq(fakeShopperId));
        verify(commandContextMock, never())
                .execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class));
    }
}