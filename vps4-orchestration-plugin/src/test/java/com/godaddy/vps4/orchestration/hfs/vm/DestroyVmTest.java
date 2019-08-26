package com.godaddy.vps4.orchestration.hfs.vm;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

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
    DestroyVm.Request destroyVmRequest;
    VmAction hfsAction;
    long hfsVmId;
    long vps4ActionId;
    Vm hfsVm;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAndRecordVmAction.class).toInstance(waitAction);
        binder.bind(HfsVmTrackingRecordService.class).toInstance(hfsTrackingService);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    DestroyVm command = new DestroyVm(vmService, hfsTrackingService);

    @Before
    public void setupTest() {
        destroyVmRequest = new DestroyVm.Request();
        hfsVmId = 23L;
        vps4ActionId = 45L;
        destroyVmRequest.hfsVmId = hfsVmId;
        destroyVmRequest.actionId = vps4ActionId;

        hfsVm = new Vm();
        hfsVm.status = "ACTIVE";
        when(vmService.getVm(hfsVmId)).thenReturn(hfsVm);

        hfsAction = mock(VmAction.class);
        when(vmService.destroyVm(hfsVmId)).thenReturn(hfsAction);
    }

    @Test
    public void callsDestroyVm() {
        command.execute(context, destroyVmRequest);
        verify(vmService).destroyVm(hfsVmId);
        verify(context).execute(WaitForAndRecordVmAction.class, hfsAction);
    }

    @Test
    public void callsUpdateHfsTrackingRecord() {
        command.execute(context, destroyVmRequest);
        verify(context).execute(eq("UpdateHfsVmTrackingRecord"),
                                          any(Function.class), eq(Void.class));
        verify(hfsTrackingService).setDestroyed(hfsVmId, destroyVmRequest.actionId);
    }

    @Test
    public void skipsDestroyIfHfsVmIdZero() {
        destroyVmRequest.hfsVmId = 0L;
        assertNull(command.execute(context, destroyVmRequest));
        verify(vmService, never()).destroyVm(hfsVmId);
        verify(hfsTrackingService, never()).setDestroyed(hfsVmId, destroyVmRequest.actionId);
    }

    @Test
    public void skipsDestroyIfHfsVmAlreadyDestroyed() {
        hfsVm.status = "DESTROYED";
        assertNull(command.execute(context, destroyVmRequest));
        verify(vmService, never()).destroyVm(hfsVmId);
        verify(hfsTrackingService, never()).setDestroyed(hfsVmId, destroyVmRequest.actionId);
    }


}
