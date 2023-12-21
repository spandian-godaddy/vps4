package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;
import com.godaddy.vps4.firewall.model.FirewallClientCreateResponse;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateCacheResponse;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
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
public class Vps4SubmitFirewallCreationTest {
    ActionService actionService = mock(ActionService.class);
    FirewallService firewallService = mock(FirewallService.class);
    FirewallDataService firewallDataService = mock(FirewallDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    NetworkService networkService = mock(NetworkService.class);
    Vps4SubmitFirewallCreation command;
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";
    String domain = "fakeDomain";

    IpAddress ipAddress = new IpAddress();

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4SubmitFirewallCreation.Request request;
    VmFirewallSite vmFirewallSite;
    FirewallClientCreateResponse response;

    @Captor private ArgumentCaptor<Function<CommandContext, FirewallClientCreateResponse>> createRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<WaitForFirewallCreationJob.Request> waitRequestArgumentCaptor;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        ipAddress.ipAddress = "fakeIpAddress";
        request = new Vps4SubmitFirewallCreation.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.vmId = vmId;
        request.shopperId = shopperId;
        request.domain = domain;
        request.ipAddress = ipAddress.ipAddress;
        request.bypassWAF = FirewallBypassWAF.ENABLED;
        request.cacheLevel = FirewallCacheLevel.CACHING_DISABLED;

        vmFirewallSite = new VmFirewallSite();
        vmFirewallSite.siteId = siteId;
        vmFirewallSite.vmId = vmId;

        response = new FirewallClientCreateResponse();
        response.siteId = "fakeSiteId";
        response.revision = 1;

        when(firewallService.createFirewall(shopperId, decryptedJwtString, domain, ipAddress,
                FirewallCacheLevel.CACHING_DISABLED.toString(), FirewallBypassWAF.ENABLED.toString())).thenReturn(response);
        when(context.execute(eq("SubmitCreateFirewall"),
                Matchers.<Function<CommandContext, FirewallClientCreateResponse>>any(),
                eq(FirewallClientCreateResponse.class)))
                .thenReturn(response);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);
        when(networkService.getActiveIpAddressOfVm(vmId, ipAddress.ipAddress)).thenReturn(ipAddress);

        command = new Vps4SubmitFirewallCreation(actionService, firewallDataService, firewallService, networkService, cryptography);
    }

    @Test
    public void testCallsNetworkService() {
        command.executeWithAction(context, request);

        verify(networkService, times(1)).getActiveIpAddressOfVm(vmId, ipAddress.ipAddress);
    }

    @Test
    public void testExecutesCreationSuccess() {
        command.executeWithAction(context, request);

        verify(context).execute(eq("SubmitCreateFirewall"), createRequestArgumentCaptor.capture(), eq(FirewallClientCreateResponse.class));

        Function<CommandContext, FirewallClientCreateResponse> createResponseContext = createRequestArgumentCaptor.getValue();
        FirewallClientCreateResponse createResponse = createResponseContext.apply(context);
        assertSame(response, createResponse);
    }

    @Test
    public void testExecutesWaitForCreationJob() {
        command.executeWithAction(context, request);

        verify(context).execute(eq(WaitForFirewallCreationJob.class), waitRequestArgumentCaptor.capture());

        WaitForFirewallCreationJob.Request req = waitRequestArgumentCaptor.getValue();
        assertEquals(shopperId, req.shopperId);
        assertEquals(siteId, req.siteId);
        assertEquals(vmId, req.vmId);
        assertEquals(encryptedJwt, req.encryptedCustomerJwt);
    }


    @Test
    public void testExecutesDeleteOnWaitForCreationJobFail() {
        when(context.execute(eq(WaitForFirewallCreationJob.class),
                any())).thenThrow(new RuntimeException("test"));

        try {
            command.executeWithAction(context, request);
            fail();
        }
        catch(RuntimeException e) {
            verify(firewallService).deleteFirewallSite(request.shopperId,
                    cryptography.decrypt(request.encryptedCustomerJwt), siteId);
        }
    }
}
