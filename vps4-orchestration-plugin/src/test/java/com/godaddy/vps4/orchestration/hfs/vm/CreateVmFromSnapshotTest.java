package com.godaddy.vps4.orchestration.hfs.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.orchestration.phase2.Vps4ExternalsModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class CreateVmFromSnapshotTest {
    static Injector injector;

    @Inject VmService vmService;

    private CreateVmFromSnapshot command;
    private CommandContext context;
    private CreateVMWithFlavorRequest request;
    private VmAction hfsAction;

    @Captor
    ArgumentCaptor<Function<CommandContext, VmAction>> createVmLambdaCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new Vps4ExternalsModule()
        );
    }

    @Before
    public void setUpTest() {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        command = new CreateVmFromSnapshot(vmService);
        context = setupMockContext();
        request = new CreateVMWithFlavorRequest();

        when(vmService.createVmWithFlavor(any(CreateVMWithFlavorRequest.class))).thenReturn(hfsAction);
    }

    private CommandContext setupMockContext() {
        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());

        hfsAction = new VmAction();
        hfsAction.vmActionId = 12345;
        hfsAction.vmId = 4567;

        when(mockContext.execute(eq("CreateVmHfs"), any(Function.class), eq(VmAction.class))).thenReturn(hfsAction);
        when(mockContext.execute(eq(WaitForVmAction.class), eq(hfsAction))).thenReturn(null);
        return mockContext;
    }

    @Test
    public void callsHfsVmVerticalToCreateTheVm() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("CreateVmHfs"), createVmLambdaCaptor.capture(), eq(VmAction.class));

        // Verify that the lambda is returning what we expect
        Function<CommandContext, VmAction> lambda = createVmLambdaCaptor.getValue();
        VmAction vmAction = lambda.apply(context);
        Assert.assertEquals(vmAction, hfsAction);
    }

    @Test
    public void waitsForCompletionOfTheVmCreation() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(WaitForVmAction.class), eq(hfsAction));
    }

    @Test
    public void commandReturnsTheHfsVmAction() {
        Assert.assertEquals(command.execute(context, request), hfsAction);
    }
}
