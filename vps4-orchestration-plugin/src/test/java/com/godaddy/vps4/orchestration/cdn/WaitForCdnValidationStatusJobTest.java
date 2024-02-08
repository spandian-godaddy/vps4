package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnCloudflareData;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnProductData;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.CdnValidation;
import com.godaddy.vps4.util.Cryptography;
import gdg.hfs.orchestration.CommandContext;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WaitForCdnValidationStatusJobTest {
    private CommandContext context;
    private CdnService cdnService;
    private WaitForCdnValidationStatusJob command;

    private CdnDetail cdnDetail;
    private final UUID vmId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    String siteId = "fakeSiteId";

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        cdnService = mock(CdnService.class);

        CdnValidation cdnValidation = new CdnValidation();
        cdnValidation.name = "validationName";
        cdnValidation.type = "validationType";
        CdnCloudflareData cloudflareData = new CdnCloudflareData();
        cloudflareData.certificateValidation = new CdnValidation[]{cdnValidation};
        CdnProductData productData = new CdnProductData(cloudflareData);
        cdnDetail = new CdnDetail();
        cdnDetail.siteId = siteId;
        cdnDetail.productData = productData;

        when(cdnService.getCdnSiteDetail(any(), anyString(), any(), anyBoolean())).thenReturn(cdnDetail);

        command = new WaitForCdnValidationStatusJob(cdnService);
    }

    @Test
    public void throwsErrorIfNewValidationInfoIsPopulated() {
        WaitForCdnValidationStatusJob.Request request = new WaitForCdnValidationStatusJob.Request();
        request.vmId = vmId;
        request.siteId = siteId;
        request.customerId = customerId;
        try {
            command.execute(context, request);
            fail();
        } catch (RuntimeException e) {
            assertEquals("Failed to complete cdn validation - more verification needed - new verification info was added", e.getMessage());
        }
    }

    @Test
    public void returnsIfStatusIsSuccess() {
        cdnDetail.status = CdnStatus.SUCCESS;
        when(cdnService.getCdnSiteDetail(any(), anyString(), any(), anyBoolean())).thenReturn(cdnDetail);

        WaitForCdnValidationStatusJob.Request request = new WaitForCdnValidationStatusJob.Request();
        request.vmId = vmId;
        request.siteId = siteId;
        request.customerId = customerId;
        request.certificateValidation = cdnDetail.productData.cloudflare.certificateValidation;

        command.execute(context, request);
        verify(cdnService, times(1)).getCdnSiteDetail(customerId, siteId, vmId, true);
    }
}
