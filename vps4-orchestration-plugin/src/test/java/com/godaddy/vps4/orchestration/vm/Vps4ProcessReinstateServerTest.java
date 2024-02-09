package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.cdn.Vps4ModifyCdnSite;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class Vps4ProcessReinstateServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;
    VirtualMachineCredit credit;
    VmCdnSite vmCdnSite = mock(VmCdnSite.class);

    @Captor
    private ArgumentCaptor<Vps4ModifyCdnSite.Request> modifyCdnSiteLambdaCaptor;

    Vps4ProcessReinstateServer command = new Vps4ProcessReinstateServer(actionService, creditService, cdnDataService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.spec = mock(ServerSpec.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vmCdnSite.siteId = "fakeSiteId";
        vmCdnSite.vmId = vm.vmId;
        credit = mock(VirtualMachineCredit.class);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);
        when(credit.getShopperId()).thenReturn("fakeShopperId");
        when(cdnDataService.getActiveCdnSitesOfVm(vm.vmId)).thenReturn(Collections.singletonList(vmCdnSite));
    }

    @Test
    public void testReinstateVirtual() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        when(credit.isAccountSuspended()).thenReturn(false);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(StartVm.class, vm.hfsVmId);
        verify(creditService, times(1)).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED,
                null);
    }

    @Test
    public void testReinstateDed() {
        when(vm.spec.isVirtualMachine()).thenReturn(false);
        when(credit.isAccountSuspended()).thenReturn(false);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(EndRescueVm.class, vm.hfsVmId);
        verify(creditService, times(1)).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED,
                null);
    }

    @Test
    public void testResumePanoptaMonitoring(){
        VmActionRequest request = new VmActionRequest();
        when(credit.isAccountSuspended()).thenReturn(false);
        request.virtualMachine = vm;
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(ResumePanoptaMonitoring.class), any());
    }

    @Test
    public void testGetsAndUnpausesCdnSites(){
        VmActionRequest request = new VmActionRequest();
        when(credit.isAccountSuspended()).thenReturn(false);
        request.virtualMachine = vm;
        command.executeWithAction(context, request);
        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vm.vmId);
        verify(context, times(1))
                .execute(eq("ModifyCdnSite-" + vmCdnSite.siteId), eq(Vps4ModifyCdnSite.class), modifyCdnSiteLambdaCaptor.capture());

        Vps4ModifyCdnSite.Request req = modifyCdnSiteLambdaCaptor.getValue();
        Assert.assertEquals(vm.vmId, req.vmId);
        Assert.assertEquals(CdnBypassWAF.DISABLED, req.bypassWAF);
        Assert.assertEquals(CdnCacheLevel.CACHING_OPTIMIZED, req.cacheLevel);
        Assert.assertEquals(null, req.encryptedCustomerJwt);
        Assert.assertEquals(credit.getShopperId(), req.shopperId);
        Assert.assertEquals(vmCdnSite.siteId, req.siteId);
    }

    @Test
    public void doesNotPauseCdnIfEmptyCdnListReturned() {
        when(cdnDataService.getActiveCdnSitesOfVm(vm.vmId)).thenReturn(Collections.emptyList());
        VmActionRequest request = new VmActionRequest();
        when(credit.isAccountSuspended()).thenReturn(false);
        request.virtualMachine = vm;
        command.executeWithAction(context, request);

        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vm.vmId);
        verify(context, times(0))
                .execute(startsWith("ModifyCdnSite-"), eq(Vps4ModifyCdnSite.class), any());
    }

    @Test
    public void doesNotPauseCdnIfNullCdnReturned() {
        when(cdnDataService.getActiveCdnSitesOfVm(vm.vmId)).thenReturn(null);
        VmActionRequest request = new VmActionRequest();
        when(credit.isAccountSuspended()).thenReturn(false);
        request.virtualMachine = vm;
        command.executeWithAction(context, request);

        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vm.vmId);
        verify(context, times(0))
                .execute(startsWith("ModifyCdnSite-"), eq(Vps4ModifyCdnSite.class), any());
    }

    @Test
    public void testReinstateFailsForSuspendedAccount() {
        when(credit.isAccountSuspended()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        try {
            command.executeWithAction(context, request);
        }
        catch (RuntimeException e) {
            verify(context, never()).execute(StartVm.class, vm.hfsVmId);
        }
    }
}
