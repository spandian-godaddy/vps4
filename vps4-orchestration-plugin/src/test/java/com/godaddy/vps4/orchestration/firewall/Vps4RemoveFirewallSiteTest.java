package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
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

public class Vps4RemoveFirewallSiteTest {
    ActionService actionService = mock(ActionService.class);
    FirewallService firewallService = mock(FirewallService.class);
    FirewallDataService firewallDataService = mock(FirewallDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    Vps4RemoveFirewallSite command = new Vps4RemoveFirewallSite(actionService, firewallDataService, firewallService, cryptography);
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4RemoveFirewallSite.Request request;
    VmFirewallSite vmFirewallSite;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4RemoveFirewallSite.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = shopperId;
        vmFirewallSite = new VmFirewallSite();
        vmFirewallSite.siteId = siteId;
        vmFirewallSite.vmId = vmId;

        when(firewallDataService.getFirewallSiteFromId(vmId, siteId)).thenReturn(vmFirewallSite);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);
    }
    
    @Test
    public void testExecuteSuccess() {
        command.execute(context, request);

        verify(firewallDataService, times(1)).getFirewallSiteFromId(vmId, siteId);
        verify(cryptography, times(1)).decrypt(encryptedJwt);
        verify(firewallService, times(1)).deleteFirewallSite(shopperId, decryptedJwtString, siteId);
        verify(firewallDataService, times(1)).destroyFirewallSite(vmId, siteId);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteFirewallNotFound() {
        when(firewallDataService.getFirewallSiteFromId(vmId, siteId)).thenReturn(null);
        command.execute(context, request);
    }
}
