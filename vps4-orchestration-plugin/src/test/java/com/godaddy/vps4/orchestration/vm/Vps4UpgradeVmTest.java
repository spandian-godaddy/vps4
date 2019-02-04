package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class Vps4UpgradeVmTest {
    ActionService actionService = mock(ActionService.class);
    @SnapshotActionService ActionService snapshotActionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    SnapshotService vps4SnapshotService = mock(SnapshotService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    ProjectService projectService = mock(ProjectService.class);
    CreditService creditService = mock(CreditService.class);
    long oldHfsVmId = 123L;
    long actionId = 456L;
    UUID snapshotId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();

    Vps4UpgradeVm.Request request;
    VirtualMachine vm;
    UUID existingVMId = UUID.randomUUID();

    Vps4UpgradeVm command;

    @Captor
    private ArgumentCaptor<Function<CommandContext, VirtualMachine>> getVirtualMachineLambdaCaptor;
    @Captor
    private ArgumentCaptor<Vps4SnapshotVm.Request> snapshotRequestCaptor;
    @Captor
    private ArgumentCaptor<Vps4RestoreVm.Request> restoreRequestCaptor;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4UpgradeVm.class);
        binder.bind(ActionService.class).annotatedWith(SnapshotActionService.class).toInstance(snapshotActionService);
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(SnapshotService.class).toInstance(vps4SnapshotService);
        binder.bind(VmUserService.class).toInstance(vmUserService);
        binder.bind(ProjectService.class).toInstance(projectService);
        binder.bind(CreditService.class).toInstance(creditService);
    });

    CommandContext context;

    @Before
    public void setupTest() {
        command = injector.getInstance(Vps4UpgradeVm.class);
        request = mock(Vps4UpgradeVm.Request.class);
        request.vmId = existingVMId;
        request.newTier = 40;
        context = setupMockContext();
        MockitoAnnotations.initMocks(this);
    }

    private CommandContext setupMockContext() {
        vm = mock(VirtualMachine.class);
        vm.hfsVmId = oldHfsVmId;
        vm.orionGuid = orionGuid;
        when(virtualMachineService.getVirtualMachine(eq(existingVMId))).thenReturn(vm);

        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());
        when(mockContext.execute(eq("GetHfsVmId"), any(Function.class), eq(long.class))).thenReturn(oldHfsVmId);

        when(mockContext.execute(eq("GetVirtualMachine"), any(Function.class), eq(VirtualMachine.class))).thenReturn(vm);

        when(mockContext.execute(eq("createSnapshot"), any(Function.class), eq(UUID.class))).thenReturn(snapshotId);

        when(mockContext.execute(eq("createSnapshotAction"), any(Function.class), eq(long.class))).thenReturn(actionId);

        Project project = new Project(123, "unitTestProject", "vps4-unittest-123", Instant.now(), null);
        when(projectService.getProject(eq(vm.projectId))).thenReturn(project);

        ServerSpec spec = mock(ServerSpec.class);
        spec.specName = "hosting.c2.r4.d60";
        spec.specId = 6;
        when(virtualMachineService.getSpec(eq(request.newTier))).thenReturn(spec);

        VmUser testUser = new VmUser("testUser", request.vmId, true, VmUserType.CUSTOMER);
        when(vmUserService.getPrimaryCustomer(eq(request.vmId))).thenReturn(testUser);

        return mockContext;
    }

    @Test
    public void getsVirtualMachineFromRequestVmId() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("GetVirtualMachine"), getVirtualMachineLambdaCaptor.capture(), eq(VirtualMachine.class));

        Function<CommandContext, VirtualMachine> lambda = getVirtualMachineLambdaCaptor.getValue();
        VirtualMachine retVm = lambda.apply(context);
        verify(virtualMachineService, times(1)).getVirtualMachine(eq(existingVMId));
        Assert.assertEquals(vm, retVm);
    }

    @Test
    public void createSnapshotForVm() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("Vps4SnapshotVm"), eq(Vps4SnapshotVm.class), snapshotRequestCaptor.capture());
        Vps4SnapshotVm.Request retSnapshotReq = snapshotRequestCaptor.getValue();
        Assert.assertEquals(oldHfsVmId, retSnapshotReq.hfsVmId);
        Assert.assertEquals(snapshotId, retSnapshotReq.vps4SnapshotId);
        Assert.assertEquals(vm.orionGuid, retSnapshotReq.orionGuid);
        Assert.assertEquals(SnapshotType.AUTOMATIC, retSnapshotReq.snapshotType);
        Assert.assertEquals(request.shopperId, retSnapshotReq.shopperId);
        Assert.assertEquals(request.initiatedBy, retSnapshotReq.initiatedBy);
        Assert.assertEquals(actionId, retSnapshotReq.actionId);
    }

    @Test
    public void createVmFromSnapshot() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("Vps4RestoreVm"), eq(Vps4RestoreVm.class), restoreRequestCaptor.capture());
        Vps4RestoreVm.Request retRestoreReq = restoreRequestCaptor.getValue();
        Assert.assertEquals(request.actionId, retRestoreReq.actionId);
        Assert.assertEquals(request.vmId, retRestoreReq.restoreVmInfo.vmId);
        Assert.assertEquals(snapshotId, retRestoreReq.restoreVmInfo.snapshotId);
        Assert.assertEquals("vps4-unittest-123", retRestoreReq.restoreVmInfo.sgid);
        Assert.assertEquals(vm.hostname, retRestoreReq.restoreVmInfo.hostname);
        Assert.assertEquals(request.encryptedPassword, retRestoreReq.restoreVmInfo.encryptedPassword);
        Assert.assertEquals("hosting.c2.r4.d60", retRestoreReq.restoreVmInfo.rawFlavor);
        Assert.assertEquals("testUser", retRestoreReq.restoreVmInfo.username);
        Assert.assertEquals(request.zone, retRestoreReq.restoreVmInfo.zone);
    }

    @Test
    public void updateVmSpecInDb() {
        command.execute(context, request);
        verify(context, times(1))
                .execute(eq("UpdateVmTier"), any(Function.class), eq(Void.class));
    }

    @Test
    public void updateProductMetaInEcomm() {
        command.execute(context, request);
        verify(creditService, times(1))
                .updateProductMeta(eq(vm.orionGuid), eq(ECommCreditService.ProductMetaField.PLAN_CHANGE_PENDING), eq("false"));
    }
}