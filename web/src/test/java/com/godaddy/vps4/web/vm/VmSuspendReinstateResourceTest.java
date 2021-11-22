package com.godaddy.vps4.web.vm;

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
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        return new VirtualMachineCredit.Builder(dataCenterService)
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .build();
    }

    private VirtualMachineCredit createCreditWithProductMeta(AccountStatus accountStatus,
                                                             Map<String, String> productMeta) {
        return new VirtualMachineCredit.Builder(dataCenterService)
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .withProductMeta(productMeta)
                .build();
    }

    @Test
    public void testAbuseSuspendVmInvokesCommand() {
        createTestVm();

        vmSuspendReinstateResource.abuseSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testAbuseSuspendVmCreatesAction() {
        createTestVm();

        vmSuspendReinstateResource.abuseSuspendAccount(testVm.vmId);

        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test
    public void testAbuseSuspendDedicatedInvokesCommand() {
        createTestVm(ServerType.Type.DEDICATED);

        vmSuspendReinstateResource.abuseSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendDedServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testAbuseSuspendDedicatedCreatesAction() {
        createTestVm(ServerType.Type.DEDICATED);

        vmSuspendReinstateResource.abuseSuspendAccount(testVm.vmId);

        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testAbuseSuspendVmCreditNotFound() {
        createTestVm();
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vmSuspendReinstateResource.abuseSuspendAccount(testVm.vmId);
    }

    @Test
    public void testAbuseSuspendVmIfCreditAlreadySuspended() {
        createTestVm();
        credit = createCredit(AccountStatus.SUSPENDED);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.abuseSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.ABUSE_SUSPEND), anyObject(), anyString());
    }

    @Test
    public void testBillingSuspendVmExecutesCommand() {
        createTestVm();

        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testBillingSuspendVmCreatesAction() {
        createTestVm();

        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);

        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.BILLING_SUSPEND), anyObject(), anyString());
    }

    @Test
    public void testBillingSuspendDedicatedExecutesCommand() {
        createTestVm(ServerType.Type.DEDICATED);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendDedServer", argument.getValue().commands.get(0).command);
    }

    @Test
    public void testBillingSuspendDedicatedCreatesAction() {
        createTestVm(ServerType.Type.DEDICATED);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);

        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.BILLING_SUSPEND), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testBillingSuspendVmCreditNotFound() {
        createTestVm();
        when(creditService.getVirtualMachineCredit(any())).thenReturn(null);
        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);
    }

    @Test
    public void testBillingSuspendVmIfCreditAlreadySuspended() {
        createTestVm();
        credit = createCredit(AccountStatus.SUSPENDED);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.BILLING_SUSPEND), anyObject(), anyString());
    }

    @Test
    public void testBillingSuspendVmIfCreditAlreadyAbuseSuspended() {
        createTestVm();
        credit = createCredit(AccountStatus.SUSPENDED);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.billingSuspendAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4SuspendServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.BILLING_SUSPEND), anyObject(), anyString());
        verify(creditService, never()).updateProductMeta(any(UUID.class), any(), anyString());
    }

    @Test
    public void testReinstateAbuseSuspendedVm() {
        createTestVm();
        credit = createCreditWithProductMeta(AccountStatus.ABUSE_SUSPENDED,
                                             Collections.singletonMap(ProductMetaField.ABUSE_SUSPENDED_FLAG
                                                                              .toString(), String.valueOf(true)));
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.reinstateAbuseSuspendedAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ReinstateServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
    }

    @Test
    public void testReinstateAbuseSuspendedDedicated() {
        createTestVm(ServerType.Type.DEDICATED);
        credit = createCreditWithProductMeta(AccountStatus.ABUSE_SUSPENDED,
                                             Collections.singletonMap(ProductMetaField.ABUSE_SUSPENDED_FLAG
                                                                              .toString(), String.valueOf(true)));
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.reinstateAbuseSuspendedAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ReinstateDedServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
        verify(creditService, times(1)).getVirtualMachineCredit(eq(orionGuid));
    }

    @Test(expected = Vps4Exception.class)
    public void testDoesNotReinstateBillingSuspendedVmFlaggedForAbuse() {
        createTestVm();
        credit = createCreditWithProductMeta(AccountStatus.SUSPENDED,
                                             Collections.singletonMap(ProductMetaField.ABUSE_SUSPENDED_FLAG
                                                                              .toString(), String.valueOf(true)));
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.reinstateBillingSuspendedAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, never()).executeCommand(argument.capture());
        verify(actionService, never())
                .createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
        verify(creditService, never())
                .updateProductMeta(eq(orionGuid), eq(ProductMetaField.ABUSE_SUSPENDED_FLAG), eq("false"));
        verify(creditService, never())
                .updateProductMeta(eq(orionGuid), eq(ProductMetaField.BILLING_SUSPENDED_FLAG), eq("false"));
    }

    @Test
    public void testReinstateBillingSuspendedVm() {
        createTestVm();
        credit = createCreditWithProductMeta(AccountStatus.SUSPENDED,
                                             Collections.singletonMap(ProductMetaField.BILLING_SUSPENDED_FLAG
                                                                              .toString(), String.valueOf(true)));
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.reinstateBillingSuspendedAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ReinstateServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
    }

    @Test
    public void testReinstateBillingSuspendedDedicated() {
        createTestVm(ServerType.Type.DEDICATED);
        credit = createCreditWithProductMeta(AccountStatus.SUSPENDED,
                                             Collections.singletonMap(ProductMetaField.BILLING_SUSPENDED_FLAG
                                                                              .toString(), String.valueOf(true)));
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);

        vmSuspendReinstateResource.reinstateBillingSuspendedAccount(testVm.vmId);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4ReinstateDedServer", argument.getValue().commands.get(0).command);
        verify(actionService, times(1))
                .createAction(eq(testVm.vmId), eq(ActionType.REINSTATE), anyObject(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testDoesNotReinstateVmThatWasNotSuspended() {
        createTestVm();
        vmSuspendReinstateResource.reinstateAbuseSuspendedAccount(testVm.vmId);
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
