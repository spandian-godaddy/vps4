package com.godaddy.vps4.orchestration.panopta;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;

import gdg.hfs.orchestration.CommandContext;

public class GetPanoptaServerDetailsTest {

    private PanoptaService panoptaServiceMock;
    private PanoptaDataService panoptaDataServiceMock;
    private CommandContext contextMock;
    private GetPanoptaServerDetails command;
    private GetPanoptaServerDetails.Request request;
    private PanoptaCustomer panoptaCustomerMock;
    private PanoptaServer panoptaServerMock;
    private UUID fakeVmId = UUID.randomUUID();
    private String fakePartnerCustomerKey = "gdtest_" + fakeVmId;

    @Before
    public void setUp() throws Exception {
        panoptaDataServiceMock = mock(PanoptaDataService.class);
        panoptaServiceMock = mock(PanoptaService.class);
        command = new GetPanoptaServerDetails(panoptaServiceMock, panoptaDataServiceMock);
        contextMock = mock(CommandContext.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        panoptaServerMock = mock(PanoptaServer.class);
        setupMockContext();
        setupCommandRequest();
    }

    private void setupMockContext() {
        try {
            when(panoptaServiceMock.getServer(eq(fakePartnerCustomerKey))).thenReturn(panoptaServerMock);
        } catch (PanoptaServiceException psex) {
            fail("Unexpected Exception during test setup " + psex);
        }
        when(contextMock.execute(eq("CreatePanoptaDetailsInVPS4Db"), any(Function.class), eq(PanoptaServer.class))).thenReturn(null);
    }

    private void setupCommandRequest() {
        request = new GetPanoptaServerDetails.Request();
        request.panoptaCustomer = panoptaCustomerMock;
        request.vmId = fakeVmId;
        request.partnerCustomerKey = fakePartnerCustomerKey;
    }

    @Test
    public void invokesGetServerOnPanoptaService() {
        command.execute(contextMock, request);
        try {
            verify(panoptaServiceMock, times(1)).getServer(eq(fakePartnerCustomerKey));
        } catch (PanoptaServiceException psex) {
            fail("Unexpected exception encountered. " + psex);
        }
    }

    @Test
    public void savesPanoptaDetailsInVps4Db() {
        command.execute(contextMock, request);
        verify(contextMock, times(1)).execute(eq("CreatePanoptaDetailsInVPS4Db"), any(Function.class), eq(Void.class));
    }


}