package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallCloudflareData;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallProductData;
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
public class WaitForFirewallCreationJobTest {
    private CommandContext context;
    private FirewallService firewallService;
    private Cryptography cryptography;
    private WaitForFirewallCreationJob command;

    private FirewallDetail firewallDetail;
    private final UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";
    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    @Before
    public void setUp() throws Exception {
        context = mock(CommandContext.class);
        firewallService = mock(FirewallService.class);
        cryptography = mock(Cryptography.class);

        FirewallValidation firewallValidation = new FirewallValidation();
        firewallValidation.name = "validationName";
        firewallValidation.type = "validationType";
        FirewallCloudflareData cloudflareData = new FirewallCloudflareData();
        cloudflareData.certificateValidation = new FirewallValidation[]{firewallValidation};
        FirewallProductData productData = new FirewallProductData(cloudflareData);
        firewallDetail = new FirewallDetail();
        firewallDetail.siteId = siteId;
        firewallDetail.productData = productData;

        when(firewallService.getFirewallSiteDetail(anyString(), anyString(), anyString(), any(), anyBoolean())).thenReturn(firewallDetail);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);

        command = new WaitForFirewallCreationJob(firewallService, cryptography);
    }

    @Test
    public void returnsIfValidationInfoIsPopulated() {
        WaitForFirewallCreationJob.Request request = new WaitForFirewallCreationJob.Request();
        request.vmId = vmId;
        request.siteId = siteId;
        request.shopperId = shopperId;
        request.encryptedCustomerJwt = encryptedJwtString.getBytes();
        command.execute(context, request);
        verify(firewallService, times(1)).getFirewallSiteDetail(shopperId, decryptedJwtString, siteId, vmId, true);
    }
}
