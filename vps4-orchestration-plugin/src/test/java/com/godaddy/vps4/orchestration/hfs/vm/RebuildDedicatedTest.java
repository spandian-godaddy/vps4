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

import com.godaddy.hfs.vm.RebuildDedicatedRequest;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class RebuildDedicatedTest {
    static Injector injector;

    @Inject
    private VmService vmService;
    @Inject
    private Cryptography cryptography;
    private CommandContext rebuildDedicatedContext;
    private RebuildDedicated.Request request;
    private VmAction vmAction;
    private RebuildDedicated rebuildDedicatedCommand;

    @Captor
    private ArgumentCaptor<Function<CommandContext, VmAction>> rebuildDedicatedLambdaCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        vmService = mock(VmService.class);
        cryptography = mock(Cryptography.class);
        rebuildDedicatedContext = mock(CommandContext.class);

        vmAction = new VmAction();
        vmAction.vmActionId = 777;
        vmAction.vmId = 666;

        injector = Guice.createInjector(binder ->  {
            binder.bind(VmService.class).toInstance(vmService);
            binder.bind(Cryptography.class).toInstance(cryptography);
        });

        rebuildDedicatedCommand = new RebuildDedicated(vmService, cryptography);
        request = new RebuildDedicated.Request();
    }

    @Test
    public void testRebuildDedicated() {
        when(vmService.rebuildVm(anyLong(), any(RebuildDedicatedRequest.class))).thenReturn(vmAction);
        when(rebuildDedicatedContext.execute(eq("RebuildDedicated"), any(Function.class), eq(VmAction.class))).thenReturn(vmAction);
        when(rebuildDedicatedContext.execute(eq(WaitForVmAction.class), eq(vmAction))).thenReturn(null);

        rebuildDedicatedCommand.execute(rebuildDedicatedContext, request);

        verify(rebuildDedicatedContext, times(1))
                .execute(eq("RebuildDedicated"), rebuildDedicatedLambdaCaptor.capture(), eq(VmAction.class));

        Function<CommandContext, VmAction> lambda = rebuildDedicatedLambdaCaptor.getValue();
        VmAction actualVmAction = lambda.apply(rebuildDedicatedContext);
        assertEquals(vmAction, actualVmAction);
    }
}