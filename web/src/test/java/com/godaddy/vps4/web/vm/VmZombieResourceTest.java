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

import com.godaddy.vps4.vm.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmZombieResourceTest {

    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    CommandService commandService = mock(CommandService.class);
    ActionService actionService = mock(ActionService.class);

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

        oldCredit = createOldCredit(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);

        UUID newOrionGuid = UUID.randomUUID();
        newCredit = createNewCredit(oldCredit, newOrionGuid);
        when(creditService.getVirtualMachineCredit(newOrionGuid)).thenReturn(newCredit);

        Action testAction = new Action(123L, testVm.vmId, ActionType.CANCEL_ACCOUNT, null, null, null,
                ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(), null);

        when(actionService.getAction(anyLong())).thenReturn(testAction);
        vmZombieResource = new VmZombieResource(virtualMachineService, creditService, commandService, user, actionService);
    }

    private VirtualMachineCredit createNewCredit(VirtualMachineCredit oldCredit, UUID newOrionGuid) {
        VirtualMachineCredit newCredit = new VirtualMachineCredit();
        newCredit.orionGuid = newOrionGuid;
        newCredit.shopperId = user.getShopperId();
        newCredit.accountStatus = AccountStatus.ACTIVE;
        newCredit.controlPanel = oldCredit.controlPanel;
        newCredit.managedLevel = oldCredit.managedLevel;
        newCredit.monitoring = oldCredit.monitoring;
        newCredit.operatingSystem = oldCredit.operatingSystem;
        newCredit.tier = oldCredit.tier;
        return newCredit;
    }

    private VirtualMachineCredit createOldCredit(VirtualMachine testVm) {
        VirtualMachineCredit oldCredit = new VirtualMachineCredit();
        oldCredit.orionGuid = testVm.orionGuid;
        oldCredit.accountStatus = AccountStatus.REMOVED;
        oldCredit.shopperId = user.getShopperId();
        oldCredit.controlPanel = "cpanel";
        oldCredit.managedLevel = 0;
        oldCredit.monitoring = 1;
        oldCredit.operatingSystem = "linux";
        oldCredit.tier = 10;
        return oldCredit;
    }

    @Test
    public void testReviveZombieVm() {
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
        verify(commandService, times(1)).executeCommand(anyObject());
    }

    @Test
    public void testOldCreditNotRemoved() {
        oldCredit.accountStatus = AccountStatus.ACTIVE;
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_STATUS_NOT_REMOVED", e.getId());
        }
    }

    @Test
    public void testNewCreditInUse() {
        newCredit.provisionDate = Instant.now();
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_ALREADY_IN_USE", e.getId());
        }
    }

    @Test
    public void testControlPanelsDontMatch() {
        newCredit.controlPanel = "myh";
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONTROL_PANEL_MISMATCH", e.getId());
        }
    }

    @Test
    public void testManagedLevelsDontMatch() {
        newCredit.managedLevel = 2;
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("MANAGED_LEVEL_MISMATCH", e.getId());
        }
    }

    @Test
    public void testMonitoringsDontMatch() {
        newCredit.monitoring = 0;
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("MONITORING_MISMATCH", e.getId());
        }
    }

    @Test
    public void testOperatingSystemsDontMatch() {
        newCredit.operatingSystem = "windows";
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("OPERATING_SYSTEM_MISMATCH", e.getId());
        }
    }

    @Test
    public void testTiersDontMatch() {
        newCredit.tier = 20;
        try {
            vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("TIER_MISMATCH", e.getId());
        }
    }

    @Test
    public void testZombieVm() {
        vmZombieResource.zombieVm(testVm.vmId);
        verify(commandService, times(1)).executeCommand(anyObject());
    }

    @Test
    public void testCreditNotRemoved() {
        oldCredit.accountStatus = AccountStatus.ACTIVE;
        try {
            vmZombieResource.zombieVm(testVm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_STATUS_NOT_REMOVED", e.getId());
        }
    }
}
