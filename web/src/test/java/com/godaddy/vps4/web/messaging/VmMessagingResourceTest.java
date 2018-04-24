package com.godaddy.vps4.web.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.messaging.DefaultVps4MessagingService.EmailTemplates;
import com.godaddy.vps4.orchestration.messaging.FailOverEmailRequest;
import com.godaddy.vps4.orchestration.messaging.ScheduledMaintenanceEmailRequest;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmMessagingResourceTest {
    private VmMessagingResource resource;

    private VirtualMachineService virtualMachineService;
    private Vps4UserService vps4UserService;
    private CommandService commandService;

    VirtualMachine vm;
    Vps4User user;

    @Before
    public void setupMocks() {
        virtualMachineService = mock(VirtualMachineService.class);
        vps4UserService = mock(Vps4UserService.class);
        commandService = mock(CommandService.class);

        vm = new VirtualMachine();
        vm.vmId = UUID.randomUUID();
        vm.managedLevel = 0;
        vm.validUntil = Instant.MAX;
        vm.name = "testVmName";

        user = new Vps4User(1L, "testMessagingUser");

        CommandState command = new CommandState();
        command.commandId = UUID.randomUUID();

        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(virtualMachineService.getUserIdByVmId(vm.vmId)).thenReturn(user.getId());
        when(vps4UserService.getUser(1L)).thenReturn(user);
        when(commandService.executeCommand(any())).thenReturn(command);

        resource = new VmMessagingResource(virtualMachineService, vps4UserService, commandService);
    }

    @Test
    public void testMessagePatching() {
        long duration = 24L * 60L;
        Instant startTime = Instant.now();
        resource.messagePatching(vm.vmId, startTime.toString(), duration);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        ScheduledMaintenanceEmailRequest request = (ScheduledMaintenanceEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(duration, request.durationMinutes);
        assertEquals(vm.isFullyManaged(), request.isFullyManaged);
        assertEquals(user.getShopperId(), request.shopperId);
        assertEquals(startTime, request.startTime);
        assertEquals(EmailTemplates.VPS4ScheduledPatchingV2, request.template);
    }

    @Test(expected = Vps4Exception.class)
    public void testMessageInvalidDuration() {
        long duration = -1L;
        Instant startTime = Instant.now();
        resource.messagePatching(vm.vmId, startTime.toString(), duration);

        verify(commandService, never()).executeCommand(any());
    }

    @Test(expected = Vps4Exception.class)
    public void testMessageInvalidStartTime() {
        long duration = 1L;
        String startTime = "bad time";
        resource.messagePatching(vm.vmId, startTime, duration);

        verify(commandService, never()).executeCommand(any());
    }

    @Test(expected = NotFoundException.class)
    public void testMessageNoVm() {
        long duration = 24L * 60L;
        Instant startTime = Instant.now();
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(null);
        resource.messagePatching(vm.vmId, startTime.toString(), duration);
    }

    @Test
    public void testMessageMaintenance() {
        long duration = 24L * 60L;
        Instant startTime = Instant.now();
        resource.messageScheduledMaintenance(vm.vmId, startTime.toString(), duration);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        ScheduledMaintenanceEmailRequest request = (ScheduledMaintenanceEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(duration, request.durationMinutes);
        assertEquals(vm.isFullyManaged(), request.isFullyManaged);
        assertEquals(user.getShopperId(), request.shopperId);
        assertEquals(startTime, request.startTime);
        assertEquals(EmailTemplates.VPS4UnexpectedbutScheduledMaintenanceV2, request.template);
    }

    @Test
    public void testMessageFailover() {
        resource.messageFailover(vm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        FailOverEmailRequest request = (FailOverEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(vm.isFullyManaged(), request.isFullyManaged);
        assertEquals(user.getShopperId(), request.shopperId);
        assertEquals(EmailTemplates.VPS4SystemDownFailoverV2, request.template);
    }

    @Test
    public void testMessageFailoverComplete() {
        resource.messageFailoverComplete(vm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        FailOverEmailRequest request = (FailOverEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(vm.isFullyManaged(), request.isFullyManaged);
        assertEquals(user.getShopperId(), request.shopperId);
        assertEquals(EmailTemplates.VPS4UnexpectedscheduledmaintenanceFailoveriscompleted, request.template);
    }

}
