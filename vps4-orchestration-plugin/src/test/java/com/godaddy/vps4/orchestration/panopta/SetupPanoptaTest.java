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

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanopta;
import com.godaddy.vps4.panopta.PanoptaCustomer;
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
    private SetupPanopta command;
    private SetupPanopta.Request request;
    private UUID fakeVmId;
    private UUID fakeOrionGuid;
    private long fakeHfsVmId;
    private String fakePanoptaTemplates;
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
        fakePanoptaTemplates = "super-fake-panopta-template";
        creditServiceMock = mock(CreditService.class);
        commandContextMock = mock(CommandContext.class);
        createPanoptaCustomerMock = mock(CreatePanoptaCustomer.class);
        getPanoptaServerDetailsMock = mock(GetPanoptaServerDetails.class);
        installPanoptaMock = mock(InstallPanopta.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        panoptaServerMock = mock(PanoptaServer.class);

        command = new SetupPanopta(creditServiceMock);

        setupFakePanoptaCustomerResponse();
        setupFakePanoptaServerResponse();
        setupMockContext();
        setupCommandRequest();
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

    @Test
    public void invokesCreatePanoptaCustomer() {
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), any(CreatePanoptaCustomer.Request.class));
        verify(commandContextMock, times(1))
                .execute(eq(CreatePanoptaCustomer.class), createPanoptaRequestCaptor.capture());
        CreatePanoptaCustomer.Request capturedRequest = createPanoptaRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", capturedRequest.vmId, fakeVmId);
    }

    @Test
    public void invokesGetPanoptaServerDetails() {
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1))
                .execute(eq(GetPanoptaServerDetails.class), getPanoptaDetailsRequestCaptor.capture());
        GetPanoptaServerDetails.Request capturedRequest = getPanoptaDetailsRequestCaptor.getValue();
        assertEquals("Expected vm id in request does not match actual value. ", capturedRequest.vmId, fakeVmId);
        assertEquals("Expected panopta customer in request does not match actual value. ", capturedRequest.panoptaCustomer, panoptaCustomerMock);
    }

    @Test
    public void invokesInstallPanopta() {
        command.execute(commandContextMock, request);

        verify(commandContextMock, times(1)).execute(eq(InstallPanopta.class), installPanoptaRequestCaptor.capture());
        InstallPanopta.Request capturedRequest = installPanoptaRequestCaptor.getValue();
        assertEquals("Expected HFS vm id in request does not match actual value. ", capturedRequest.hfsVmId, fakeHfsVmId);
    }
}