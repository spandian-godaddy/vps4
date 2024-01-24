package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.VmCdnSite;
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

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Vps4ClearCdnCacheTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    Vps4ClearCdnCache command;
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4ClearCdnCache.Request request;
    VmCdnSite vmCdnSite;
    CdnClientInvalidateCacheResponse response;

    @Captor private ArgumentCaptor<Function<CommandContext, CdnClientInvalidateCacheResponse>> invalidateRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<WaitForCdnClearCacheJob.Request> waitRequestArgumentCaptor;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4ClearCdnCache.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = shopperId;
        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = siteId;
        vmCdnSite.vmId = vmId;

        response = new CdnClientInvalidateCacheResponse();
        response.invalidationId = "fakeInvalidationId";

        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(vmCdnSite);
        when(context.execute(eq("ClearCdnCache"),
                Matchers.<Function<CommandContext, CdnClientInvalidateCacheResponse>>any(),
                eq(CdnClientInvalidateCacheResponse.class)))
                .thenReturn(response);
        when(cryptography.decryptIgnoreNull(any())).thenReturn(decryptedJwtString);
        when(cdnService.invalidateCdnCache(shopperId, decryptedJwtString, siteId)).thenReturn(response);

        command = new Vps4ClearCdnCache(actionService, cdnDataService, cdnService, cryptography);
    }

    @Test
    public void testExecutesClearCdnCacheSuccess() {
        command.executeWithAction(context, request);

        verify(context).execute(eq("ClearCdnCache"), invalidateRequestArgumentCaptor.capture(), eq(CdnClientInvalidateCacheResponse.class));

        Function<CommandContext, CdnClientInvalidateCacheResponse> invalidateResponseContext = invalidateRequestArgumentCaptor.getValue();
        CdnClientInvalidateCacheResponse invalidateCacheResponse = invalidateResponseContext.apply(context);
        assertEquals(response.invalidationId, invalidateCacheResponse.invalidationId);
    }

    @Test
    public void testExecutesWaitForClearCacheJob() {
        command.executeWithAction(context, request);

        verify(context).execute(eq(WaitForCdnClearCacheJob.class), waitRequestArgumentCaptor.capture());

        WaitForCdnClearCacheJob.Request req = waitRequestArgumentCaptor.getValue();
        assertEquals(shopperId, req.shopperId);
        assertEquals(siteId, req.siteId);
        assertEquals(response.invalidationId, req.validationId);
        assertEquals(encryptedJwt, req.encryptedCustomerJwt);
    }
}
