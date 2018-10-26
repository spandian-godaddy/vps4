package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmSuspendResourceTest {

    VmResource vmResource = mock(VmResource.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    ActionService actionService = mock(ActionService.class);
    CommandService commandService = mock(CommandService.class);
    VmActionResource vmActionResource = mock(VmActionResource.class);
    VirtualMachine testVm;
    VirtualMachineCredit credit;
    VmSuspendResource vmSuspendResource;
    Action testAction;

    private GDUser user;

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        createTestVm();
        when(vmResource.getVm(testVm.vmId)).thenReturn(testVm);

        credit = createCredit(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(credit);

        testAction = new Action(123L, testVm.vmId, ActionType.ABUSE_SUSPEND, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);

        when(actionService.getAction(anyLong())).thenReturn(testAction);
        when(actionService.createAction(testVm.vmId, ActionType.REINSTATE, null, user.getUsername()))
                .thenReturn(testAction.id);
        vmSuspendResource = new VmSuspendResource(user, vmResource, creditService, actionService, commandService,
                vmActionResource, virtualMachineService);
    }

    private void createTestVm() {
        testVm = new VirtualMachine();
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = UUID.randomUUID();
        testVm.canceled = Instant.now().plus(7, ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);
    }

    private VirtualMachineCredit createCredit(VirtualMachine testVm) {
        VirtualMachineCredit credit = new VirtualMachineCredit();
        credit.orionGuid = testVm.orionGuid;
        credit.accountStatus = AccountStatus.ACTIVE;
        credit.shopperId = user.getShopperId();
        return credit;
    }

    @Test
    public void testAbuseSuspendVm() {
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
        verify(commandService, times(1)).executeCommand(anyObject());
        verify(actionService, times(1)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testAbuseSuspendVmCreditNotFounc() {
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
        verify(commandService, times(0)).executeCommand(anyObject());
        verify(actionService, times(0)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testAbuseSuspendVmCreditNotActive() {
        credit.accountStatus = AccountStatus.SUSPENDED;
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        vmSuspendResource.abuseSuspendVm(testVm.vmId);
        verify(commandService, times(0)).executeCommand(anyObject());
        verify(actionService, times(0)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test
    public void testReinstateVm() {
        credit.accountStatus = AccountStatus.ABUSE_SUSPENDED;
        vmSuspendResource.reinstateVm(testVm.vmId);
        verify(creditService, times(1)).setStatus(testVm.orionGuid, AccountStatus.ACTIVE);
        verify(actionService, times(1)).createAction(testVm.vmId, ActionType.REINSTATE,
                null, user.getUsername());
        verify(actionService, times(1)).completeAction(testAction.id, null, null);
    }

    @Test(expected = Vps4Exception.class)
    public void testReinstateVmNotSuspended() {
        vmSuspendResource.reinstateVm(testVm.vmId);
        verify(creditService, times(0)).setStatus(testVm.orionGuid, AccountStatus.ACTIVE);
        verify(actionService, times(0)).createAction(testVm.vmId, ActionType.REINSTATE,
                null, user.getUsername());
        verify(actionService, times(0)).completeAction(testAction.id, null, null);
    }
}
