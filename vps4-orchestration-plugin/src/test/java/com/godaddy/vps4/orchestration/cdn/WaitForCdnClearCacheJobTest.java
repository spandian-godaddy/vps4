package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateStatusResponse;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.util.Cryptography;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WaitForCdnClearCacheJobTest {
    private CommandContext context;
    private CdnService cdnService;
    private Cryptography cryptography;
    private WaitForCdnClearCacheJob command;

    CdnClientInvalidateStatusResponse response;
    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";
    String validationId = "fakeValidationId";
    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        cdnService = mock(CdnService.class);
        cryptography = mock(Cryptography.class);

        response = new CdnClientInvalidateStatusResponse();
        response.status = CdnStatus.SUCCESS;
        response.message = "success";

        when(cdnService.getCdnInvalidateCacheStatus(any(), any(), any(), any())).thenReturn(response);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);

        command = new WaitForCdnClearCacheJob(cdnService, cryptography);
    }

    @Test
    public void returnsIfStatusIsSuccess() {
        WaitForCdnClearCacheJob.Request request = new WaitForCdnClearCacheJob.Request();
        request.siteId = siteId;
        request.validationId = validationId;
        request.shopperId = shopperId;
        request.encryptedCustomerJwt = encryptedJwtString.getBytes();
        command.execute(context, request);
        verify(cdnService, times(1)).getCdnInvalidateCacheStatus(shopperId, decryptedJwtString, siteId, validationId);
    }
}
