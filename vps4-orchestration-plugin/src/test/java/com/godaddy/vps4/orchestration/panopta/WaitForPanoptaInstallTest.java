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

public class WaitForPanoptaInstallTest {

    private PanoptaService panoptaServiceMock;
    private PanoptaDataService panoptaDataServiceMock;
    private CommandContext contextMock;
    private WaitForPanoptaInstall command;
    private WaitForPanoptaInstall.Request request;
    private PanoptaCustomer panoptaCustomerMock;
    private PanoptaServer panoptaServerMock;
    private UUID fakeVmId = UUID.randomUUID();
    private String fakeShopperId = "fake-shopper-id";
    private String fakePartnerCustomerKey = "gdtest_" + fakeShopperId;
    private String fakeServerKey = "fake-server-key";

    @Before
    public void setUp() throws Exception {
        panoptaDataServiceMock = mock(PanoptaDataService.class);
        panoptaServiceMock = mock(PanoptaService.class);
        command = new WaitForPanoptaInstall(panoptaServiceMock);
        contextMock = mock(CommandContext.class);
        panoptaCustomerMock = mock(PanoptaCustomer.class);
        panoptaServerMock = mock(PanoptaServer.class);
        setupMockContext();
        setupCommandRequest();
    }

    private void setupMockContext() {
        try {
            when(panoptaServiceMock.getServer(eq(fakeShopperId), eq(fakeServerKey))).thenReturn(panoptaServerMock);
        } catch (PanoptaServiceException psex) {
            fail("Unexpected Exception during test setup " + psex);
        }
        when(contextMock.execute(eq("CreatePanoptaDetailsInVPS4Db"), any(Function.class), eq(PanoptaServer.class))).thenReturn(null);
    }

    private void setupCommandRequest() {
        request = new WaitForPanoptaInstall.Request();
        request.vmId = fakeVmId;
        request.shopperId = fakeShopperId;
        request.serverKey = fakeServerKey;
    }

    @Test
    public void invokesGetServerOnPanoptaService() {
        command.execute(contextMock, request);
        try {
            verify(panoptaServiceMock, times(1)).getServer(eq(fakeShopperId), eq(fakeServerKey));
        } catch (PanoptaServiceException psex) {
            fail("Unexpected exception encountered. " + psex);
        }
    }
}