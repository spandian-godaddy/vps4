package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4MoveOutTest {
    private CommandContext context;
    private ActionService actionService;
    private VirtualMachineService virtualMachineService;
    private VmUserService vmUserService;
    private SchedulerWebService schedulerWebService;
    private Vps4MoveOut command;

    private Vps4MoveOut.Request request;
    private List<VmUser> supportUsers;

    @Captor private ArgumentCaptor<String> commandNameCaptor;
    @Captor private ArgumentCaptor<Vps4RemoveSupportUser.Request> removeSupportUserRequestCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        context = mock(CommandContext.class);
        actionService = mock(ActionService.class);
        virtualMachineService = mock(VirtualMachineService.class);
        vmUserService = mock(VmUserService.class);
        schedulerWebService = mock(SchedulerWebService.class);
        command = new Vps4MoveOut(actionService, virtualMachineService, vmUserService, schedulerWebService);

        request = new Vps4MoveOut.Request();
        request.vmId = UUID.randomUUID();
        request.backupJobId = UUID.randomUUID();
        request.hfsVmId = 42L;

        VmUser[] supportUserArray = new VmUser[] {
                new VmUser("lobster", request.vmId, true, VmUserType.SUPPORT),
                new VmUser("duck", request.vmId, true, VmUserType.SUPPORT)
        };
        supportUsers = Arrays.asList(supportUserArray);

        when(context.getId()).thenReturn(UUID.randomUUID());
        when(vmUserService.listUsers(request.vmId, VmUserType.SUPPORT)).thenReturn(supportUsers);
    }

    @Test
    public void removesAllSupportUsers() {
        List<String> capturedCommandNames;
        List<Vps4RemoveSupportUser.Request> capturedRequests;

        command.execute(context, request);

        verify(context, times(2)).execute(commandNameCaptor.capture(),
                eq(Vps4RemoveSupportUser.class), removeSupportUserRequestCaptor.capture());
        capturedCommandNames = commandNameCaptor.getAllValues();
        capturedRequests = removeSupportUserRequestCaptor.getAllValues();

        assertEquals("RemoveUser-lobster", capturedCommandNames.get(0));
        assertEquals("RemoveUser-duck", capturedCommandNames.get(1));
        assertEquals("lobster", capturedRequests.get(0).username);
        assertEquals("duck", capturedRequests.get(1).username);
    }

    @Test
    public void pausesAutomaticBackups() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq("PauseAutomaticBackups"),
                any(Function.class), eq(Void.class));
    }

    @Test
    public void doesNotPauseAutomaticBackupsForNullBackupJobId() {
        request.backupJobId = null;
        command.execute(context, request);
        verify(schedulerWebService, never()).pauseJob("vps4", "backups", request.backupJobId);
    }

    @Test
    public void callsToSetCanceledAndValidUntil() {
        command.execute(context, request);

        verify(context, times(1)).execute(eq("MarkVmAsZombie"),
                any(Function.class), eq(Void.class));
        verify(context, times(1)).execute(eq("MarkVmAsRemoved"),
                any(Function.class), eq(Void.class));
    }

    @Test
    public void pausesPanoptaMonitoring() {
        command.execute(context, request);
        verify(context, times(1)).execute(eq(PausePanoptaMonitoring.class), eq(request.vmId));
    }
}
