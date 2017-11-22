package com.godaddy.vps4.web.vm;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmZombieResourceTest {

    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    VmResource vmResource = mock(VmResource.class);
    CreditService creditService = mock(CreditService.class);
    CommandService commandService = mock(CommandService.class);
    
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
        when(vmResource.getVm(testVm.vmId)).thenReturn(testVm);
        when(virtualMachineService.getVirtualMachine(testVm.vmId)).thenReturn(testVm);

        oldCredit = createOldCredit(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);
        
        UUID newOrionGuid = UUID.randomUUID();
        newCredit = createNewCredit(oldCredit, newOrionGuid);
        when(creditService.getVirtualMachineCredit(newOrionGuid)).thenReturn(newCredit);
        
        vmZombieResource = new VmZombieResource(user, virtualMachineService, vmResource, creditService, commandService);
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

    @Test(expected = Vps4Exception.class)
    public void testOldCreditNotRemoved() {
        oldCredit.accountStatus = AccountStatus.ACTIVE;
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }

    @Test(expected = Vps4Exception.class)
    public void testNewCreditInUser() {
        newCredit.provisionDate = Instant.now();
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }

    @Test(expected = Vps4Exception.class)
    public void testControlPanelsDontMatch() {
        newCredit.controlPanel = "myh";
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }

    @Test(expected = Vps4Exception.class)
    public void testManagedLevelsDontMatch() {
        newCredit.managedLevel = 2;
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }

    @Test(expected = Vps4Exception.class)
    public void testMonitoringsDontMatch() {
        newCredit.monitoring = 0;
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }

    @Test(expected = Vps4Exception.class)
    public void testOperatingSystemsDontMatch() {
        newCredit.operatingSystem = "windows";
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }

    @Test(expected = Vps4Exception.class)
    public void testTiersDontMatch() {
        newCredit.tier = 20;
        vmZombieResource.reviveZombieVm(testVm.vmId, newCredit.orionGuid);
    }
}
