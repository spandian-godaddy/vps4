package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import static org.mockito.internal.verification.VerificationModeFactory.times;

public class Vps4RemoveCdnSiteTest {
    ActionService actionService = mock(ActionService.class);
    CdnService cdnService = mock(CdnService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    Vps4RemoveCdnSite command = new Vps4RemoveCdnSite(actionService, cdnDataService, cdnService, cryptography);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4RemoveCdnSite.Request request;
    VmCdnSite vmCdnSite;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4RemoveCdnSite.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = shopperId;
        vmCdnSite = new VmCdnSite();
        vmCdnSite.siteId = siteId;
        vmCdnSite.vmId = vmId;

        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(vmCdnSite);
        when(cryptography.decryptIgnoreNull(any())).thenReturn(decryptedJwtString);
    }
    
    @Test
    public void testExecuteSuccess() {
        command.execute(context, request);

        verify(cdnDataService, times(1)).getCdnSiteFromId(vmId, siteId);
        verify(cryptography, times(1)).decryptIgnoreNull(encryptedJwt);
        verify(cdnService, times(1)).deleteCdnSite(shopperId, decryptedJwtString, siteId);
        verify(cdnDataService, times(1)).destroyCdnSite(vmId, siteId);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteCdnNotFound() {
        when(cdnDataService.getCdnSiteFromId(vmId, siteId)).thenReturn(null);
        command.execute(context, request);
    }
}
