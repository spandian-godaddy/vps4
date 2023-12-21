package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateCacheResponse;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
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
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class Vps4ClearFirewallCacheTest {
    ActionService actionService = mock(ActionService.class);
    FirewallService firewallService = mock(FirewallService.class);
    FirewallDataService firewallDataService = mock(FirewallDataService.class);
    Cryptography cryptography = mock(Cryptography.class);
    Vps4ClearFirewallCache command;
    CommandContext context = mock(CommandContext.class);

    UUID vmId = UUID.randomUUID();

    String encryptedJwtString = "encryptedJwt";
    String siteId = "fakeSiteId";

    String shopperId = "fakeShopperId";
    String decryptedJwtString = "decryptedJwt";

    byte[] encryptedJwt = encryptedJwtString.getBytes();
    Vps4ClearFirewallCache.Request request;
    VmFirewallSite vmFirewallSite;
    FirewallClientInvalidateCacheResponse response;

    @Captor private ArgumentCaptor<Function<CommandContext, FirewallClientInvalidateCacheResponse>> invalidateRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<WaitForFirewallClearCacheJob.Request> waitRequestArgumentCaptor;

    @Before
    public void setUp() {
        when(context.getId()).thenReturn(UUID.randomUUID());
        request = new Vps4ClearFirewallCache.Request();
        request.encryptedCustomerJwt = encryptedJwt;
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = shopperId;
        vmFirewallSite = new VmFirewallSite();
        vmFirewallSite.siteId = siteId;
        vmFirewallSite.vmId = vmId;

        response = new FirewallClientInvalidateCacheResponse();
        response.invalidationId = "fakeInvalidationId";

        when(firewallDataService.getFirewallSiteFromId(vmId, siteId)).thenReturn(vmFirewallSite);
        when(context.execute(eq("ClearFirewallCache"),
                Matchers.<Function<CommandContext, FirewallClientInvalidateCacheResponse>>any(),
                eq(FirewallClientInvalidateCacheResponse.class)))
                .thenReturn(response);
        when(cryptography.decrypt(any())).thenReturn(decryptedJwtString);
        when(firewallService.invalidateFirewallCache(shopperId, decryptedJwtString, siteId)).thenReturn(response);

        command = new Vps4ClearFirewallCache(actionService, firewallDataService, firewallService, cryptography);
    }

    @Test
    public void testExecutesClearFireWallCacheSuccess() {
        command.executeWithAction(context, request);

        verify(context).execute(eq("ClearFirewallCache"), invalidateRequestArgumentCaptor.capture(), eq(FirewallClientInvalidateCacheResponse.class));

        Function<CommandContext, FirewallClientInvalidateCacheResponse> invalidateResponseContext = invalidateRequestArgumentCaptor.getValue();
        FirewallClientInvalidateCacheResponse invalidateCacheResponse = invalidateResponseContext.apply(context);
        assertEquals(response.invalidationId, invalidateCacheResponse.invalidationId);
    }

    @Test
    public void testExecutesWaitForClearCacheJob() {
        command.executeWithAction(context, request);

        verify(context).execute(eq(WaitForFirewallClearCacheJob.class), waitRequestArgumentCaptor.capture());

        WaitForFirewallClearCacheJob.Request req = waitRequestArgumentCaptor.getValue();
        assertEquals(shopperId, req.shopperId);
        assertEquals(siteId, req.siteId);
        assertEquals(response.invalidationId, req.validationId);
        assertEquals(encryptedJwt, req.encryptedCustomerJwt);
    }
}
