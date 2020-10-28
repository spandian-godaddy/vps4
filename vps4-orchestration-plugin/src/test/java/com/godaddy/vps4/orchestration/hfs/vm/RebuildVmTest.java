package com.godaddy.vps4.orchestration.hfs.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.hfs.vm.RebuildVmRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class RebuildVmTest {
    static Injector injector;

    @Inject
    private VmService vmService;
    @Inject
    private Cryptography cryptography;
    private CommandContext context;
    private RebuildVm.Request request;
    private VmAction vmAction;
    private RebuildVm rebuildVmCommand;

    @Captor
    private ArgumentCaptor<Function<CommandContext, VmAction>> rebuildVmLambdaCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        vmService = mock(VmService.class);
        cryptography = mock(Cryptography.class);
        context = mock(CommandContext.class);

        vmAction = new VmAction();
        vmAction.vmActionId = 777;
        vmAction.vmId = 666;

        injector = Guice.createInjector(binder ->  {
            binder.bind(VmService.class).toInstance(vmService);
            binder.bind(Cryptography.class).toInstance(cryptography);
        });

        rebuildVmCommand = new RebuildVm(vmService, cryptography);
        request = new RebuildVm.Request();
    }

    @Test
    public void testRebuildVm() {
        when(vmService.rebuildVm(anyLong(), any(RebuildVmRequest.class))).thenReturn(vmAction);
        when(context.execute(eq("RebuildVmHfs"), any(Function.class), eq(VmAction.class))).thenReturn(vmAction);
        when(context.execute(eq(WaitForVmAction.class), eq(vmAction))).thenReturn(null);

        rebuildVmCommand.execute(context, request);

        verify(context, times(1))
                .execute(eq("RebuildVmHfs"), rebuildVmLambdaCaptor.capture(), eq(VmAction.class));

        Function<CommandContext, VmAction> lambda = rebuildVmLambdaCaptor.getValue();
        VmAction actualVmAction = lambda.apply(context);
        assertEquals(vmAction, actualVmAction);
    }
}