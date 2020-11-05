package com.godaddy.vps4.orchestration.hfs.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.hfs.vm.ResizeRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class ResizeOHVmTest {
    VmService hfsVmService = mock(VmService.class);
    WaitForVmAction waitAction = mock(WaitForVmAction.class);
    long hfsVmId = 23L;
    String specName = "oh.hosting.c4.r16.d200";

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForVmAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));
    ResizeOHVm command = new ResizeOHVm(hfsVmService);

    @Test
    public void testExecute() {
        VmAction hfsUpgradeAction = mock(VmAction.class);
        ResizeRequest hfsRequest = new ResizeRequest();
        hfsRequest.rawFlavor = specName;
        when(hfsVmService.resize(eq(hfsVmId), any(ResizeRequest.class))).thenReturn(hfsUpgradeAction);
        ResizeOHVm.Request resizeOHVmRequest = new ResizeOHVm.Request(hfsVmId, specName);
        VmAction response = command.execute(context, resizeOHVmRequest);
        assertEquals(hfsUpgradeAction, response);
        verify(context).execute(WaitForVmAction.class, hfsUpgradeAction);
    }
}
