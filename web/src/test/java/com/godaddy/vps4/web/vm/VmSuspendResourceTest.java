package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import junit.framework.Assert;

public class VmSuspendResourceTest {

    VmResource vmResource = mock(VmResource.class);
    CreditService creditService = mock(CreditService.class);
    ActionService actionService = mock(ActionService.class);
    CommandService commandService = mock(CommandService.class);
    VirtualMachine testVm;
    VirtualMachineCredit credit;
    VmSuspendResource vmSuspendResource;
    Action testAction;

    private GDUser user = GDUserMock.createShopper();
    private UUID orionGuid = UUID.randomUUID();
    private UUID vmId = UUID.randomUUID();

    @Before
    public void setupTest() {
        credit = createCredit();
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);

        testAction = new Action(123L, vmId, ActionType.ABUSE_SUSPEND, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);
        when(actionService.getAction(anyLong())).thenReturn(testAction);
        when(actionService.createAction(vmId, ActionType.REINSTATE, null, user.getUsername())).thenReturn(testAction.id);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        vmSuspendResource = new VmSuspendResource(user, vmResource, creditService, actionService, commandService);
    }

    private void createTestVm() {
        createTestVm(ServerType.Type.VIRTUAL);
    }

    private void createTestVm(ServerType.Type serverType) {
        ServerSpec testSpec = new ServerSpec();
        testSpec.serverType = new ServerType();
        testSpec.serverType.serverType = serverType;

        testVm = new VirtualMachine();
        testVm.vmId = vmId;
        testVm.orionGuid = orionGuid;
        testVm.canceled = Instant.now().plus(7, ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;
        testVm.spec = testSpec;
        when(vmResource.getVm(testVm.vmId)).thenReturn(testVm);
    }

    private VirtualMachineCredit createCredit() {
        VirtualMachineCredit credit = new VirtualMachineCredit();
        credit.orionGuid = orionGuid;
        credit.accountStatus = AccountStatus.ACTIVE;
        credit.shopperId = user.getShopperId();
        return credit;
    }

    @Test
    public void testAbuseSuspendVm() {
        createTestVm();
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4AbuseSuspendVm", argument.getValue().commands.get(0).command);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test
    public void testAbuseSuspendDedicated() {
        createTestVm(ServerType.Type.DEDICATED);
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4AbuseSuspendDedicated", argument.getValue().commands.get(0).command);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testAbuseSuspendVmCreditNotFound() {
        createTestVm();
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
    }

    @Test(expected = Vps4Exception.class)
    public void testAbuseSuspendVmCreditNotActive() {
        createTestVm();
        credit.accountStatus = AccountStatus.SUSPENDED;
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
    }

    @Test
    public void testReinstateVm() {
        createTestVm();
        credit.accountStatus = AccountStatus.ABUSE_SUSPENDED;

        vmSuspendResource.reinstateVm(testVm.vmId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ReinstateVm", argument.getValue().commands.get(0).command);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
    }

    @Test
    public void testReinstateDedicated() {
        createTestVm(ServerType.Type.DEDICATED);
        credit.accountStatus = AccountStatus.ABUSE_SUSPENDED;

        vmSuspendResource.reinstateVm(testVm.vmId);
        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ReinstateDedicated", argument.getValue().commands.get(0).command);
        verify(actionService, times(1)).createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testReinstateVmNotSuspended() {
        createTestVm();
        vmSuspendResource.reinstateVm(testVm.vmId);
    }
}
