package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateStatusResponse;
import com.godaddy.vps4.firewall.model.FirewallCloudflareData;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallProductData;
import com.godaddy.vps4.firewall.model.FirewallStatus;
import com.godaddy.vps4.firewall.model.FirewallValidation;
import com.godaddy.vps4.util.Cryptography;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WaitForFirewallClearCacheJobTest {
    private CommandContext context;
    private FirewallService firewallService;
    private Cryptography cryptography;
    private WaitForFirewallClearCacheJob command;

    FirewallClientInvalidateStatusResponse response;
    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";
    String validationId = "fakeValidationId";
    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        firewallService = mock(FirewallService.class);
        cryptography = mock(Cryptography.class);

        response = new FirewallClientInvalidateStatusResponse();
        response.status = FirewallStatus.SUCCESS;
        response.message = "success";

        when(firewallService.getFirewallInvalidateCacheStatus(any(), any(), any(), any())).thenReturn(response);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);

        command = new WaitForFirewallClearCacheJob(firewallService, cryptography);
    }

    @Test
    public void returnsIfStatusIsSuccess() {
        WaitForFirewallClearCacheJob.Request request = new WaitForFirewallClearCacheJob.Request();
        request.siteId = siteId;
        request.validationId = validationId;
        request.shopperId = shopperId;
        request.encryptedCustomerJwt = encryptedJwtString.getBytes();
        command.execute(context, request);
        verify(firewallService, times(1)).getFirewallInvalidateCacheStatus(shopperId, decryptedJwtString, siteId, validationId);
    }
}
