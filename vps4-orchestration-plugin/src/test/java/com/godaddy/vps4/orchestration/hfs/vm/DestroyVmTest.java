package com.godaddy.vps4.orchestration.hfs.vm;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class DestroyVmTest {

    VmService vmService = mock(VmService.class);
    HfsVmTrackingRecordService hfsTrackingService = mock(HfsVmTrackingRecordService.class);
    WaitForAndRecordVmAction waitAction = mock(WaitForAndRecordVmAction.class);
    VmAction hfsAction;
    Long hfsVmId;
    Vm hfsVm;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAndRecordVmAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    DestroyVm command = new DestroyVm(vmService, hfsTrackingService);

    @Before
    public void setupTest() {
        hfsVmId = 23L;

        hfsVm = new Vm();
        hfsVm.status = "ACTIVE";
        when(vmService.getVm(hfsVmId)).thenReturn(hfsVm);

        hfsAction = mock(VmAction.class);
        when(vmService.destroyVm(hfsVmId)).thenReturn(hfsAction);
    }

    @Test
    public void callsDestroyVm() {
        command.execute(context, hfsVmId);
        verify(vmService).destroyVm(hfsVmId);
        verify(context).execute(WaitForAndRecordVmAction.class, hfsAction);
        verify(hfsTrackingService).setCanceled(hfsVmId);
    }

    @Test
    public void skipsDestroyIfHfsVmIdZero() {
        hfsVmId = 0L;
        assertNull(command.execute(context, hfsVmId));
        verify(vmService, never()).destroyVm(hfsVmId);
    }

    @Test
    public void skipsDestroyIfHfsVmAlreadyDestroyed() {
        hfsVm.status = "DESTROYED";
        assertNull(command.execute(context, hfsVmId));
        verify(vmService, never()).destroyVm(hfsVmId);
    }

}
