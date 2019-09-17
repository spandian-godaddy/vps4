package com.godaddy.vps4.orchestration.panopta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;

import gdg.hfs.orchestration.CommandContext;

@RunWith(MockitoJUnitRunner.class)
public class SetupPanoptaTest {

    private CreditService creditServiceMock;
    private CommandContext commandContextMock;
    private PanoptaServer panoptaServerMock;
    private PanoptaCustomer panoptaCustomerMock;
    private CreatePanoptaCustomer createPanoptaCustomerMock;
    private CreatePanoptaCustomer.Response createPanoptaCustomerResponse;
    private GetPanoptaServerDetails getPanoptaServerDetailsMock;
    private GetPanoptaServerDetails.Response getPanoptaServerDetailsResponse;
    private InstallPanopta installPanoptaMock;
    private PanoptaDataService panoptaDataServiceMock;
    private PanoptaDetail panoptaDetailMock;
    private Config configMock;
    private SetupPanopta command;
    private SetupPanopta.Request request;
    private UUID fakeVmId;
    private UUID fakeOrionGuid;
    private long fakeHfsVmId;
    private String fakeCustomerKey;
    private String fakePanoptaTemplates;
    private String fakeServerKey;
    private VirtualMachineCredit creditMock;
    @Captor
    private ArgumentCaptor<CreatePanoptaCustomer.Request> createPanoptaRequestCaptor;
    @Captor
    private ArgumentCaptor<InstallPanopta.Request> installPanoptaRequestCaptor;
    @Captor
    private ArgumentCaptor<GetPanoptaServerDetails.Request> getPanoptaDetailsRequestCaptor;

    @Before
    public void setUp() throws Exception {
        fakeVmId = UUID.randomUUID();
        fakeOrionGuid = UUID.randomUUID();
        fakeHfsVmId = 1234L;
        fakeCustomerKey = "so-very-fake-customer-key";
        fakePanoptaTemplates = "super-fake-panopta-template";
        creditServiceMock = mock(CreditService.class);
        commandContextMock = mock(CommandContext.class);
        createPanoptaCustomerMock = mock(CreatePanoptaCustomer.class);
        getPanoptaServerDetailsMock = mock(GetPanoptaServerDetails.class);
        installPanoptaMock = mock(InstallPanopta.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        panoptaServerMock = mock(PanoptaServer.class);
        panoptaDataServiceMock = mock(PanoptaDataService.class);
        configMock = mock(Config.class);
        creditMock = mock(VirtualMachineCredit.class);
        panoptaDetailMock = mock(PanoptaDetail.class);

        command = new SetupPanopta(creditServiceMock, panoptaDataServiceMock, configMock);

        setupFakePanoptaCustomerResponse();
        setupFakePanoptaServerResponse();
        setupMockContext();
        setupCommandRequest();
        setupMocksForTests();
    }

    private void setupMockContext() {
        when(commandContextMock.execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class)))
                .thenReturn(createPanoptaCustomerResponse);
        when(commandContextMock.execute(eq(GetPanoptaServerDetails.class), any(GetPanoptaServerDetails.Request.class)))
                .thenReturn(getPanoptaServerDetailsResponse);
    }

    private void setupCommandRequest() {
        request = new SetupPanopta.Request();
        request.vmId = fakeVmId;
        request.orionGuid = fakeOrionGuid;
        request.hfsVmId = fakeHfsVmId;
        request.panoptaTemplates = fakePanoptaTemplates;
    }

    private void setupFakePanoptaCustomerResponse() {
        createPanoptaCustomerResponse = new CreatePanoptaCustomer.Response();
        createPanoptaCustomerResponse.panoptaCustomer = panoptaCustomerMock;
    }

    private void setupFakePanoptaServerResponse() {
        getPanoptaServerDetailsResponse = new GetPanoptaServerDetails.Response();
        getPanoptaServerDetailsResponse.panoptaServer = panoptaServerMock;
    }

    private void setupMocksForTests() {
        when(creditServiceMock.getVirtualMachineCredit(eq(fakeOrionGuid))).thenReturn(creditMock);
        when(creditMock.getOperatingSystem()).thenReturn("linux");
        when(creditMock.effectiveManagedLevel()).thenReturn(VirtualMachineCredit.EffectiveManagedLevel.FULLY_MANAGED);
        when(configMock.get(eq("panopta.api.templates."
                                       + VirtualMachineCredit.EffectiveManagedLevel.FULLY_MANAGED.toString()
                                       + ".linux"))).thenReturn(fakePanoptaTemplates);
    }

    @Test
    public void invokesCreatePanoptaCustomer() {
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class));
        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), createPanoptaRequestCaptor.capture());
        CreatePanoptaCustomer.Request capturedRequest = createPanoptaRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", fakeVmId, capturedRequest.vmId);
    }

    @Test
    public void invokesGetPanoptaServerDetails() {
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(GetPanoptaServerDetails.class), getPanoptaDetailsRequestCaptor.capture());
        GetPanoptaServerDetails.Request capturedRequest = getPanoptaDetailsRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", fakeVmId, capturedRequest.vmId);
        assertEquals("Expected panopta customer in request does not match actual value. ", panoptaCustomerMock,
                     capturedRequest.panoptaCustomer);
    }

    @Test
    public void invokesInstallPanopta() {
        when(panoptaCustomerMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals("Expected HFS vm id in request does not match actual value. ", fakeHfsVmId,
                     capturedRequest.hfsVmId);
        assertEquals(fakeCustomerKey, capturedRequest.customerKey);
        assertEquals(fakePanoptaTemplates, capturedRequest.templates);
    }

    @Test
    public void invokesInstallPanoptaOnRebuilds() {
        when(panoptaDataServiceMock.getPanoptaDetails(eq(fakeVmId))).thenReturn(panoptaDetailMock);
        when(panoptaDetailMock.getCustomerKey()).thenReturn(fakeCustomerKey);
        when(panoptaDetailMock.getServerKey()).thenReturn(fakeServerKey);
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals("Expected HFS vm id in request does not match actual value. ", capturedRequest.hfsVmId,
                     fakeHfsVmId);
        assertEquals(fakeCustomerKey, capturedRequest.customerKey);
        assertEquals(fakeServerKey, capturedRequest.serverKey);
        assertEquals(fakePanoptaTemplates, capturedRequest.templates);
    }
}