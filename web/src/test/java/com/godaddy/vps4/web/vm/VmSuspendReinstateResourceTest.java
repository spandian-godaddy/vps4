package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import junit.framework.Assert;

public class VmSuspendReinstateResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private CreditService creditService = mock(CreditService.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private DataCenterService dataCenterService = mock(DataCenterService.class);
    private VirtualMachine testVm;
    private VirtualMachineCredit credit;
    private VmSuspendReinstateResource vmSuspendReinstateResource;

    private GDUser user = GDUserMock.createShopper();
    private UUID orionGuid = UUID.randomUUID();
    private UUID vmId = UUID.randomUUID();

    @Before
    public void setupTest() {
        credit = createCredit(AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);

        Action testAction = new Action(123L, vmId, ActionType.ABUSE_SUSPEND, null, null, null,
                                       ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(),
                                       null);
        when(actionService.getAction(anyLong())).thenReturn(testAction);
        when(actionService.createAction(vmId, ActionType.REINSTATE, null, user.getUsername()))
                .thenReturn(testAction.id);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        vmSuspendReinstateResource =
                new VmSuspendReinstateResource(user, vmResource, creditService, actionService, commandService,
                                               virtualMachineService);
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
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);
    }

    private VirtualMachineCredit createCredit(AccountStatus accountStatus) {
        return new VirtualMachineCredit.Builder()
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .build();
    }

    @Test
    public void testSuspend() {
        createTestVm();

        vmSuspendReinstateResource.suspendVm(testVm.vmId, "FRAUD");

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SubmitSuspendServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testReinstate() {
        createTestVm();
        vmSuspendReinstateResource.reinstateVm(testVm.vmId, "FRAUD");

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SubmitReinstateServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testProcessSuspendMessage() {
        createTestVm();

        vmSuspendReinstateResource.processSuspendMessage(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ProcessSuspendServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testProcessReinstateMessage() {
        createTestVm();

        vmSuspendReinstateResource.processReinstateMessage(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ProcessReinstateServer", argument.getValue().commands.get(0).command);
    }
}
