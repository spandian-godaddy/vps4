package com.godaddy.vps4.web.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.messaging.FailOverEmailRequest;
import com.godaddy.vps4.orchestration.messaging.ScheduledMaintenanceEmailRequest;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmMessagingResourceTest {
    private VmMessagingResource resource;

    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private CommandService commandService;

    VirtualMachine vm;
    GDUser gdUser;

    @Before
    public void setupMocks() {
        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        commandService = mock(CommandService.class);

        vm = new VirtualMachine();
        vm.vmId = UUID.randomUUID();
        vm.managedLevel = 0;
        vm.validUntil = Instant.MAX;
        vm.name = "testVmName";

        gdUser = GDUserMock.createShopper();

        CommandState command = new CommandState();
        command.commandId = UUID.randomUUID();

        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(vm);
        when(commandService.executeCommand(any())).thenReturn(command);

        VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
        when(credit.isManaged()).thenReturn(false);
        when(credit.getShopperId()).thenReturn(GDUserMock.DEFAULT_SHOPPER);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        resource = new VmMessagingResource(virtualMachineService, commandService, creditService, gdUser);
    }

    @Test
    public void testMessagePatching() {
        long duration = 24L * 60L;
        Instant startTime = Instant.now();
        ScheduledMessagingResourceRequest messagingRequest = new ScheduledMessagingResourceRequest(startTime.toString(), duration);
        resource.messagePatching(vm.vmId, messagingRequest);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        ScheduledMaintenanceEmailRequest request = (ScheduledMaintenanceEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(duration, request.durationMinutes);
        assertEquals(false, request.isManaged);
        assertEquals(GDUserMock.DEFAULT_SHOPPER, request.shopperId);
        assertEquals(startTime, request.startTime);
    }

    @Test(expected = Vps4Exception.class)
    public void testMessageInvalidDuration() {
        long duration = -1L;
        Instant startTime = Instant.now();
        ScheduledMessagingResourceRequest messagingRequest = new ScheduledMessagingResourceRequest(startTime.toString(),
                duration);
        resource.messagePatching(vm.vmId, messagingRequest);

        verify(commandService, never()).executeCommand(any());
    }

    @Test(expected = Vps4Exception.class)
    public void testMessageInvalidStartTime() {
        long duration = 1L;
        String startTime = "bad time";
        ScheduledMessagingResourceRequest messagingRequest = new ScheduledMessagingResourceRequest(startTime,
                duration);
        resource.messagePatching(vm.vmId, messagingRequest);

        verify(commandService, never()).executeCommand(any());
    }

    @Test(expected = NotFoundException.class)
    public void testMessageNoVm() {
        long duration = 24L * 60L;
        Instant startTime = Instant.now();
        when(virtualMachineService.getVirtualMachine(vm.vmId)).thenReturn(null);
        ScheduledMessagingResourceRequest messagingRequest = new ScheduledMessagingResourceRequest(startTime.toString(),
                duration);
        resource.messagePatching(vm.vmId, messagingRequest);
    }

    @Test
    public void testMessageMaintenance() {
        long duration = 24L * 60L;
        Instant startTime = Instant.now();
        ScheduledMessagingResourceRequest messagingRequest = new ScheduledMessagingResourceRequest(startTime.toString(), duration);
        resource.messageScheduledMaintenance(vm.vmId, messagingRequest);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        ScheduledMaintenanceEmailRequest request = (ScheduledMaintenanceEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(duration, request.durationMinutes);
        assertEquals(false, request.isManaged);
        assertEquals(GDUserMock.DEFAULT_SHOPPER, request.shopperId);
        assertEquals(startTime, request.startTime);
    }

    @Test
    public void testMessageFailover() {
        resource.messageFailover(vm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        FailOverEmailRequest request = (FailOverEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(false, request.isManaged);
        assertEquals(GDUserMock.DEFAULT_SHOPPER, request.shopperId);
    }

    @Test
    public void testMessageFailoverComplete() {
        resource.messageFailoverComplete(vm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        CommandGroupSpec cgs = argument.getValue();
        FailOverEmailRequest request = (FailOverEmailRequest) cgs.commands.get(0).request;
        assertEquals(vm.name, request.accountName);
        assertEquals(false, request.isManaged);
        assertEquals(GDUserMock.DEFAULT_SHOPPER, request.shopperId);
    }

}
