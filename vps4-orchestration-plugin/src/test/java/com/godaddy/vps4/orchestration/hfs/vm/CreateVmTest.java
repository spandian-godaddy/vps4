package com.godaddy.vps4.orchestration.hfs.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import com.godaddy.vps4.util.Cryptography;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class CreateVmTest {
    static Injector injector;

    @Inject private VmService vmService;
    private Cryptography cryptography;
    private CreateVm createVmCommand;
    private CommandContext createVmContext;
    private CreateVm.Request request;
    private VmAction vmAction;

    @Captor private ArgumentCaptor<Function<CommandContext, VmAction>> createVmLambdaCaptor;
    @Captor private ArgumentCaptor<VmAction> vmActionArgumentCaptor;

    @Before
    public void setup () {
        MockitoAnnotations.initMocks(this);
        vmService = mock(VmService.class);
        cryptography = mock(Cryptography.class);
        createVmContext = mock(CommandContext.class);

        vmAction = new VmAction();
        vmAction.vmActionId = 777;
        vmAction.vmId = 666;

        injector = Guice.createInjector(binder ->  {
           binder.bind(VmService.class).toInstance(vmService);
           binder.bind(Cryptography.class).toInstance(cryptography);
        });

        createVmCommand = new CreateVm(vmService, cryptography);
        request = new CreateVm.Request();
    }

    @Test
    public void testCreateVm() {
        when(vmService.createVmWithFlavor(any(CreateVMWithFlavorRequest.class))).thenReturn(vmAction);
        when(createVmContext.execute(eq("CreateVmHfs"), any(Function.class), eq(VmAction.class))).thenReturn(vmAction);
        when(createVmContext.execute(eq(WaitForVmAction.class), eq(vmAction))).thenReturn(null);

        createVmCommand.execute(createVmContext, request);

        verify(createVmContext, times(1))
                .execute(eq("CreateVmHfs"), createVmLambdaCaptor.capture(), eq(VmAction.class));

        Function<CommandContext, VmAction> lambda = createVmLambdaCaptor.getValue();
        VmAction actualVmAction = lambda.apply(createVmContext);
        Assert.assertEquals(vmAction, actualVmAction);
    }

    @Test
    public void testWaitForCreateVmCompletion() {
        when(vmService.createVmWithFlavor(any(CreateVMWithFlavorRequest.class))).thenReturn(vmAction);
        when(createVmContext.execute(eq("CreateVmHfs"), any(Function.class), eq(VmAction.class))).thenReturn(vmAction);
        when(createVmContext.execute(eq(WaitForVmAction.class), eq(vmAction))).thenReturn(null);
    }
}
