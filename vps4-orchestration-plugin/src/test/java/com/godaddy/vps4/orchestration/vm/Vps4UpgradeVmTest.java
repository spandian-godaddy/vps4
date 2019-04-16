package com.godaddy.vps4.orchestration.vm;

import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField.PLAN_CHANGE_PENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;

public class Vps4UpgradeVmTest {
    @SnapshotActionService ActionService snapshotActionService = mock(ActionService.class);
    ActionService actionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    SnapshotService snapshotService = mock(SnapshotService.class);
    VmUserService vmUserService = mock(VmUserService.class);
    ProjectService projectService = mock(ProjectService.class);
    CreditService creditService = mock(CreditService.class);

    StopVm stopVm = mock(StopVm.class);
    StartVm startVm = mock(StartVm.class);
    Vps4SnapshotVm snapshotVm = mock(Vps4SnapshotVm.class);
    Vps4RestoreVm restoreVm = mock(Vps4RestoreVm.class);

    UUID originalVmId = UUID.randomUUID();
    UUID snapshotId = UUID.randomUUID();
    UUID orionGuid = UUID.randomUUID();
    long originalHfsVmId = 123L;
    long actionId = 456L;
    long projectId = 23L;

    Vps4UpgradeVm.Request request;
    VirtualMachine vm;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(StopVm.class).toInstance(stopVm);
        binder.bind(StartVm.class).toInstance(startVm);
        binder.bind(Vps4SnapshotVm.class).toInstance(snapshotVm);
        binder.bind(Vps4RestoreVm.class).toInstance(restoreVm);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    Vps4UpgradeVm command = new Vps4UpgradeVm(snapshotActionService, actionService, virtualMachineService,
            snapshotService, vmUserService, projectService, creditService);

    @Before
    public void setupTest() {
        request = new Vps4UpgradeVm.Request();
        request.vmId = originalVmId;
        request.newTier = 40;
        request.autoBackupName = "auto-backup";
        request.initiatedBy = "Customer";

        vm = mock(VirtualMachine.class);
        vm.vmId = originalVmId;
        vm.hfsVmId = originalHfsVmId;
        vm.orionGuid = orionGuid;
        vm.projectId = projectId;
        when(virtualMachineService.getVirtualMachine(originalVmId)).thenReturn(vm);
        when(snapshotService.createSnapshot(anyLong(), any(), anyString(), any())).thenReturn(snapshotId);
        when(snapshotActionService.createAction(any(), any(), anyString(), anyString())).thenReturn(actionId);

        Project project = mock(Project.class);
        when(project.getVhfsSgid()).thenReturn("sgid-unittest");
        when(projectService.getProject(projectId)).thenReturn(project);

        ServerSpec spec = mock(ServerSpec.class);
        spec.specName = "flavor.unittest";
        spec.specId = 7;
        when(virtualMachineService.getSpec(request.newTier)).thenReturn(spec);

        VmUser vmUser = mock(VmUser.class);
        when(vmUserService.getPrimaryCustomer(originalVmId)).thenReturn(vmUser);
    }

    @Test
    public void getsVirtualMachineFromRequestVmId() {
        command.execute(context, request);
        verify(virtualMachineService).getVirtualMachine(originalVmId);
    }

    @Test
    public void stopsVmBeforeCreatingSnapshot() {
        command.execute(context, request);
        verify(context).execute(StopVm.class, originalHfsVmId);
    }

    @Test
    public void createsDbSnapshotRecord() {
        command.execute(context, request);
        verify(snapshotService).createSnapshot(projectId, originalVmId, "auto-backup", SnapshotType.AUTOMATIC);
    }

    @Test
    public void createsDbSnapshotAction() {
        command.execute(context, request);
        verify(snapshotActionService).createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "Customer");
    }

    @Test
    public void executesSnapshotVmCommand() {
        command.execute(context, request);
        ArgumentCaptor<Vps4SnapshotVm.Request> argument = ArgumentCaptor.forClass(Vps4SnapshotVm.Request.class);
        verify(context).execute(eq("Vps4SnapshotVm"), eq(Vps4SnapshotVm.class), argument.capture());
        Vps4SnapshotVm.Request snapReq = argument.getValue();
        assertEquals(snapshotId, snapReq.vps4SnapshotId);
        assertEquals(orionGuid, snapReq.orionGuid);
        assertEquals(actionId, snapReq.actionId);
        assertEquals(originalHfsVmId, snapReq.hfsVmId);
        assertEquals(SnapshotType.AUTOMATIC, snapReq.snapshotType);
        assertEquals(request.shopperId, snapReq.shopperId);
        assertEquals(request.initiatedBy, snapReq.initiatedBy);
    }

    @Test
    public void executesRestoreVmCommand() {
        command.execute(context, request);
        ArgumentCaptor<Vps4RestoreVm.Request> argument = ArgumentCaptor.forClass(Vps4RestoreVm.Request.class);
        verify(context).execute(eq("Vps4RestoreVm"), eq(Vps4RestoreVm.class), argument.capture());
        Vps4RestoreVm.Request restoreReq = argument.getValue();
        assertEquals(originalVmId, restoreReq.restoreVmInfo.vmId);
        assertEquals(snapshotId, restoreReq.restoreVmInfo.snapshotId);
        assertEquals("sgid-unittest", restoreReq.restoreVmInfo.sgid);
        assertEquals("flavor.unittest", restoreReq.restoreVmInfo.rawFlavor);
        assertEquals(request.actionId, restoreReq.actionId);
        assertEquals(orionGuid, restoreReq.restoreVmInfo.orionGuid);
        verify(virtualMachineService, atLeastOnce()).getSpec(request.newTier);
    }

    @Test
    public void startsVmOnUpgradeError() {
        doThrow(new RuntimeException("upgrade failure!"))
            .when(context).execute(eq("Vps4RestoreVm"), eq(Vps4RestoreVm.class), any());

        try {
            command.execute(context, request);
            fail();
        } catch (RuntimeException ex) {
            verify(context).execute(StartVm.class, originalHfsVmId);
        }
    }

    @Test
    public void updatesVmTierInDb() {
        command.execute(context, request);
        Map<String, Object> expectedParams = Collections.singletonMap("spec_id", 7);
        verify(virtualMachineService).updateVirtualMachine(originalVmId, expectedParams);
    }

    @Test
    public void updatesProductMetaInEcomm() {
        command.execute(context, request);
        verify(creditService).updateProductMeta(orionGuid, PLAN_CHANGE_PENDING, "false");
    }

}
