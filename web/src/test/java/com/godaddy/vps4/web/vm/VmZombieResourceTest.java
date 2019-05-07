package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.anyInt;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmZombieResourceTest {

    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    CommandService commandService = mock(CommandService.class);
    ActionService actionService = mock(ActionService.class);
    VmActionResource vmActionResource = mock(VmActionResource.class);

    VirtualMachine testVm;
    VirtualMachineCredit oldCredit;
    VirtualMachineCredit newCredit;
    VmZombieResource vmZombieResource;

    private GDUser user;

    @Before
    public void setupTest() {
        user = GDUserMock.createShopper();
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());

        testVm = new VirtualMachine();
        testVm.vmId = UUID.randomUUID();
        testVm.orionGuid = UUID.randomUUID();
        testVm.canceled = Instant.now().plus(7, ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);

        oldCredit = createOldCredit(testVm, AccountStatus.REMOVED);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);

        UUID newOrionGuid = UUID.randomUUID();
        newCredit = createNewCredit(oldCredit, newOrionGuid);
        when(creditService.getVirtualMachineCredit(newOrionGuid)).thenReturn(newCredit);

        Action testAction = new Action(123L, testVm.vmId, ActionType.CANCEL_ACCOUNT, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);

        when(actionService.getAction(anyLong())).thenReturn(testAction);
        vmZombieResource = new VmZombieResource(virtualMachineService, creditService, commandService, user, actionService, vmActionResource);
    }

    private VirtualMachineCredit createVmCredit(UUID orionGuid, AccountStatus accountStatus, String controlPanel,
        int monitoring, int managedLevel, int tier, String os, Instant provisionDate) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(tier));
        planFeatures.put("managed_level", String.valueOf(managedLevel));
        planFeatures.put("control_panel_type", String.valueOf(controlPanel));
        planFeatures.put("monitoring", String.valueOf(monitoring));
        planFeatures.put("operatingsystem", os);

        Map<String, String> productMeta = new HashMap<>();
        if (provisionDate != null)
            productMeta.put("provision_date", provisionDate.toString());

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .withProductMeta(productMeta)
                .withPlanFeatures(planFeatures)
                .build();
        return credit;
    }

    private VirtualMachineCredit createNewCredit(VirtualMachineCredit oldCredit, UUID newOrionGuid) {
        return createVmCredit(
            newOrionGuid, AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
            oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null);
    }

    private VirtualMachineCredit createOldCredit(VirtualMachine testVm, AccountStatus accountStatus) {
        return createVmCredit(
            testVm.orionGuid, accountStatus, "cpanel", 1, 0, 10, "linux", null);
    }

    @Test
    public void testReviveZombieVm() {
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
        verify(commandService, times(1)).executeCommand(anyObject());
        verify(actionService, times(1)).createAction(Matchers.eq(testVm.vmId),
                Matchers.eq(ActionType.RESTORE_ACCOUNT), anyObject(), anyString());
    }

    @Test
    public void testOldCreditNotRemoved() {
        oldCredit = createOldCredit(testVm, AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_STATUS_NOT_REMOVED", e.getId());
        }
    }

    @Test
    public void testNewCreditInUse() {
        newCredit = createVmCredit(
            newCredit.getOrionGuid(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
            oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), Instant.now());
        when(creditService.getVirtualMachineCredit(newCredit.getOrionGuid())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_ALREADY_IN_USE", e.getId());
        }
    }

    @Test
    public void testControlPanelsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getOrionGuid(), AccountStatus.ACTIVE, "myh", oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null);
        when(creditService.getVirtualMachineCredit(newCredit.getOrionGuid())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONTROL_PANEL_MISMATCH", e.getId());
        }
    }

    @Test
    public void testManagedLevelsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getOrionGuid(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                2, oldCredit.getTier(), oldCredit.getOperatingSystem(), null);
        when(creditService.getVirtualMachineCredit(newCredit.getOrionGuid())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("MANAGED_LEVEL_MISMATCH", e.getId());
        }
    }

    @Test
    public void testMonitoringsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getOrionGuid(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), 0,
                oldCredit.getManagedLevel(), oldCredit.getTier(), oldCredit.getOperatingSystem(), null);
        when(creditService.getVirtualMachineCredit(newCredit.getOrionGuid())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("MONITORING_MISMATCH", e.getId());
        }
    }

    @Test
    public void testOperatingSystemsDontMatch() {
        newCredit = createVmCredit(
                newCredit.getOrionGuid(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), oldCredit.getTier(), "windows", null);
        when(creditService.getVirtualMachineCredit(newCredit.getOrionGuid())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("OPERATING_SYSTEM_MISMATCH", e.getId());
        }
    }

    @Test
    public void testTiersDontMatch() {
        newCredit = createVmCredit(
                newCredit.getOrionGuid(), AccountStatus.ACTIVE, oldCredit.getControlPanel(), oldCredit.getMonitoring(),
                oldCredit.getManagedLevel(), 20, oldCredit.getOperatingSystem(), null);
        when(creditService.getVirtualMachineCredit(newCredit.getOrionGuid())).thenReturn(newCredit);
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.getOrionGuid());
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("TIER_MISMATCH", e.getId());
        }
    }

    @Test
    public void zombieVmKicksoffOrchEngineCommand() {
        vmZombieResource.zombieVm(testVm.vmId);
        verify(commandService, times(1)).executeCommand(anyObject());
    }

    @Test
    public void zombieVmGetsListOfCurretlyInProgressVmActions() {
        vmZombieResource.zombieVm(testVm.vmId);
        verify(actionService, times(1)).getIncompleteActions(testVm.vmId);
    }

    @Test
    public void zombieVmCancelsInProgressVmActions() {
        Action[] inCompleteActions = {mock(Action.class), mock(Action.class), mock(Action.class)};
        when(actionService.getIncompleteActions(testVm.vmId)).thenReturn(Arrays.asList(inCompleteActions));

        vmZombieResource.zombieVm(testVm.vmId);
        verify(vmActionResource, times(3)).cancelVmAction(eq(testVm.vmId), anyInt());
    }

    @Test
    public void testCreditNotRemoved() {
        oldCredit = createOldCredit(testVm, AccountStatus.ACTIVE);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);
        try {
            vmZombieResource.zombieVm(testVm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_STATUS_NOT_REMOVED", e.getId());
        }
    }
}
