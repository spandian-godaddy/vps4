package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.CdnCloudflareData;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnProductData;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.checkerframework.checker.units.qual.C;
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Vps4ValidateCdnTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    Vps4ValidateCdn command;
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4ValidateCdn.Request request;
    VmCdnSite vmCdnSite;
    CdnDetail cdnDetail;

    @Captor
    private ArgumentCaptor<WaitForCdnValidationStatusJob.Request> waitRequestArgumentCaptor;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4ValidateCdn.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.customerId = customerId;
        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = siteId;
        vmCdnSite.vmId = vmId;

        cdnDetail = new CdnDetail();
        cdnDetail.status = CdnStatus.PENDING;
        CdnProductData productData = new CdnProductData();
        productData.cloudflare = new CdnCloudflareData();
        cdnDetail.productData = productData;

        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(vmCdnSite);
        when(cdnService.getCdnSiteDetail(any(), anyString(), any(), anyBoolean())).thenReturn(cdnDetail);

        command = new Vps4ValidateCdn(actionService, cdnDataService, cdnService);
    }

    @Test
    public void testExecutesValidateCdnSuccess() {
        command.executeWithAction(context, request);

        verify(context).execute(eq("SubmitRequestCdnValidation"),
                any(Function.class), eq(Void.class));
    }

    @Test
    public void testExecutesWaitForValidationStatusJob() {
        command.executeWithAction(context, request);

        verify(context).execute(eq(WaitForCdnValidationStatusJob.class), waitRequestArgumentCaptor.capture());

        WaitForCdnValidationStatusJob.Request req = waitRequestArgumentCaptor.getValue();
        assertEquals(customerId, req.customerId);
        assertEquals(siteId, req.siteId);
    }

    @Test
    public void testReturnsIfCdnIsAlreadyValidated() {
        cdnDetail.status = CdnStatus.SUCCESS;
        when(cdnService.getCdnSiteDetail(any(), anyString(), any(), anyBoolean())).thenReturn(cdnDetail);

        command.executeWithAction(context, request);

        verify(context, times(0)).execute(eq("SubmitRequestCdnValidation"), any(Function.class), eq(Void.class));
        verify(context, times(0)).execute(eq(WaitForCdnValidationStatusJob.class), waitRequestArgumentCaptor.capture());
    }

    @Test
    public void testThrowsErrorAndQuitsGracefullyIfCdnAlreadyFailedValidation() {
        cdnDetail.status = CdnStatus.FAILED;
        when(cdnService.getCdnSiteDetail(any(), anyString(), any(), anyBoolean())).thenReturn(cdnDetail);

        try {
            command.executeWithAction(context, request);
            fail();
        } catch (RuntimeException e) {
            verify(cdnService, times(1)).deleteCdnSite(any(), anyString());
            verify(cdnDataService, times(1)).destroyCdnSite(any(), anyString());

            assertEquals("CDN status is FAILED for siteId fakeSiteId", e.getMessage());
        }

    }
}
