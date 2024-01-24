package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.CdnClientCreateResponse;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class Vps4SubmitCdnCreationTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    NetworkService networkService = mock(NetworkService.class);
    Vps4SubmitCdnCreation command;
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";
    String domain = "fakeDomain";

    IpAddress ipAddress = new IpAddress();

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4SubmitCdnCreation.Request request;
    VmCdnSite vmCdnSite;
    CdnClientCreateResponse response;

    @Captor private ArgumentCaptor<Function<CommandContext, CdnClientCreateResponse>> createRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<WaitForCdnCreationJob.Request> waitRequestArgumentCaptor;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        ipAddress.ipAddress = "fakeIpAddress";
        request = new Vps4SubmitCdnCreation.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.vmId = vmId;
        request.shopperId = shopperId;
        request.domain = domain;
        request.ipAddress = ipAddress.ipAddress;
        request.bypassWAF = CdnBypassWAF.ENABLED;
        request.cacheLevel = CdnCacheLevel.CACHING_DISABLED;

        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = siteId;
        vmCdnSite.vmId = vmId;

        response = new CdnClientCreateResponse();
        response.siteId = "fakeSiteId";
        response.revision = 1;

        when(cdnService.createCdn(shopperId, decryptedJwtString, domain, ipAddress,
                CdnCacheLevel.CACHING_DISABLED.toString(), CdnBypassWAF.ENABLED.toString())).thenReturn(response);
        when(context.execute(eq("SubmitCreateCdn"),
                Matchers.<Function<CommandContext, CdnClientCreateResponse>>any(),
                eq(CdnClientCreateResponse.class)))
                .thenReturn(response);
        when(cryptography.decryptIgnoreNull(any())).thenReturn(decryptedJwtString);
        when(networkService.getActiveIpAddressOfVm(vmId, ipAddress.ipAddress)).thenReturn(ipAddress);

        command = new Vps4SubmitCdnCreation(actionService, cdnDataService, cdnService, networkService, cryptography);
    }

    @Test
    public void testCallsNetworkService() {
        command.executeWithAction(context, request);

        verify(networkService, times(1)).getActiveIpAddressOfVm(vmId, ipAddress.ipAddress);
    }

    @Test
    public void testExecutesCreationSuccess() {
        command.executeWithAction(context, request);

        verify(context).execute(eq("SubmitCreateCdn"), createRequestArgumentCaptor.capture(), eq(CdnClientCreateResponse.class));

        Function<CommandContext, CdnClientCreateResponse> createResponseContext = createRequestArgumentCaptor.getValue();
        CdnClientCreateResponse createResponse = createResponseContext.apply(context);
        assertSame(response, createResponse);
    }

    @Test
    public void testExecutesWaitForCreationJob() {
        command.executeWithAction(context, request);

        verify(context).execute(eq(WaitForCdnCreationJob.class), waitRequestArgumentCaptor.capture());

        WaitForCdnCreationJob.Request req = waitRequestArgumentCaptor.getValue();
        assertEquals(shopperId, req.shopperId);
        assertEquals(siteId, req.siteId);
        assertEquals(vmId, req.vmId);
        assertEquals(encryptedJwt, req.encryptedCustomerJwt);
    }


    @Test
    public void testExecutesDeleteOnWaitForCreationJobFail() {
        when(context.execute(eq(WaitForCdnCreationJob.class),
                any())).thenThrow(new RuntimeException("test"));

        try {
            command.executeWithAction(context, request);
            fail();
        }
        catch(RuntimeException e) {
            verify(cdnService).deleteCdnSite(request.shopperId,
                    cryptography.decryptIgnoreNull(request.encryptedCustomerJwt), siteId);
        }
    }
}
