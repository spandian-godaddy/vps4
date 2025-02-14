package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.cdn.Vps4ModifyCdnSite;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@RunWith(MockitoJUnitRunner.class)
public class Vps4ProcessSuspendServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CdnDataService cdnDataService = mock(CdnDataService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;
    String shopperId = "test-shopper";
    UUID customerId = UUID.randomUUID();

    VmCdnSite vmCdnSite;
    VirtualMachineCredit credit;
    @Captor
    private ArgumentCaptor<Vps4ModifyCdnSite.Request> modifyCdnSiteLambdaCaptor;
    Vps4ProcessSuspendServer command = new Vps4ProcessSuspendServer(actionService, creditService, cdnDataService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.spec = mock(ServerSpec.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        vmCdnSite = mock(VmCdnSite.class);
        vmCdnSite.vmId = vm.vmId;
        vmCdnSite.siteId = "fakeSiteId";
        credit = mock(VirtualMachineCredit.class);
        when(credit.getShopperId()).thenReturn(shopperId);
        when(credit.getCustomerId()).thenReturn(customerId);
        when(cdnDataService.getActiveCdnSitesOfVm(vm.vmId)).thenReturn(Collections.singletonList(vmCdnSite));
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);
    }

    @Test
    public void testSuspendVirtual() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(StopVm.class, vm.hfsVmId);
        verify(creditService, times(1)).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED,
                Boolean.toString(true));
    }

    @Test
    public void testSuspendDed() {
        when(vm.spec.isVirtualMachine()).thenReturn(false);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(RescueVm.class, vm.hfsVmId);
        verify(creditService, times(1)).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED,
                Boolean.toString(true));
    }

    @Test
    public void testPausePanoptaMonitoring(){
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        ArgumentCaptor<PausePanoptaMonitoring.Request> pauseMonitoringCaptor = ArgumentCaptor.forClass(PausePanoptaMonitoring.Request.class);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(PausePanoptaMonitoring.class), pauseMonitoringCaptor.capture());

        PausePanoptaMonitoring.Request r = pauseMonitoringCaptor.getValue();
        assertEquals(vm.vmId, r.vmId);
        assertEquals(shopperId, r.shopperId);
    }

    @Test
    public void testPausesCdn() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vm.vmId);
        verify(context, times(1))
                .execute(eq("ModifyCdnSite-" + vmCdnSite.siteId), eq(Vps4ModifyCdnSite.class), modifyCdnSiteLambdaCaptor.capture());

        Vps4ModifyCdnSite.Request req = modifyCdnSiteLambdaCaptor.getValue();
        Assert.assertEquals(vm.vmId, req.vmId);
        Assert.assertEquals(CdnBypassWAF.ENABLED, req.bypassWAF);
        Assert.assertEquals(CdnCacheLevel.CACHING_DISABLED, req.cacheLevel);
        Assert.assertEquals(customerId, req.customerId);
        Assert.assertEquals(vmCdnSite.siteId, req.siteId);
    }

    @Test
    public void doesNotPauseCdnIfEmptyCdnListReturned() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        when(cdnDataService.getActiveCdnSitesOfVm(vm.vmId)).thenReturn(Collections.emptyList());

        command.executeWithAction(context, request);
        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vm.vmId);
        verify(context, times(0))
                .execute(startsWith("ModifyCdnSite-"), eq(Vps4ModifyCdnSite.class), any());
    }

    @Test
    public void doesNotPauseCdnIfNullCdnReturned() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        when(cdnDataService.getActiveCdnSitesOfVm(vm.vmId)).thenReturn(null);

        command.executeWithAction(context, request);
        verify(cdnDataService, times(1)).getActiveCdnSitesOfVm(vm.vmId);
        verify(context, times(0))
                .execute(startsWith("ModifyCdnSite-"), eq(Vps4ModifyCdnSite.class), any());
    }
}
