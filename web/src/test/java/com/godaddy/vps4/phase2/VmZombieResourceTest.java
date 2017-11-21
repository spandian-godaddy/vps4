package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.client.VmService;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VirtualMachineType;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmZombieResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmZombieResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject VirtualMachineService virtualMachineService;
    VmResource vmResource = Mockito.mock(VmResource.class);
    CreditService creditService = Mockito.mock(CreditService.class);
    
    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });
    
    
    private VmZombieResource vmZombieResource;

    private GDUser user;

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmZombieResource getVmZombieResource() { 
        return new VmZombieResource(user, virtualMachineService, userService, vmResource, creditService);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }
    
    @Test
    public void testReviveZombieVm() {
        VirtualMachine testVm = createTestVm();
        virtualMachineService.setVmZombie(testVm.vmId);
        Mockito.when(vmResource.getVm(testVm.vmId)).thenReturn(virtualMachineService.getVirtualMachine(testVm.vmId));

        VirtualMachineCredit oldCredit = new VirtualMachineCredit();
        oldCredit.orionGuid = testVm.orionGuid;
        oldCredit.accountStatus = AccountStatus.REMOVED;
        oldCredit.shopperId = user.getShopperId();
        oldCredit.controlPanel = "cpanel";
        oldCredit.managedLevel = 0;
        oldCredit.monitoring = 1;
        oldCredit.operatingSystem = "linux";
        oldCredit.tier = 10;
        Mockito.when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(oldCredit);
        
        UUID newOrionGuid = UUID.randomUUID();
        VirtualMachineCredit newCredit = new VirtualMachineCredit();
        newCredit.orionGuid = newOrionGuid;
        newCredit.shopperId = user.getShopperId();
        newCredit.accountStatus = AccountStatus.ACTIVE;
        newCredit.controlPanel = oldCredit.controlPanel;
        newCredit.managedLevel = oldCredit.managedLevel;
        newCredit.monitoring = oldCredit.monitoring;
        newCredit.operatingSystem = oldCredit.operatingSystem;
        newCredit.tier = oldCredit.tier;
        Mockito.when(creditService.getVirtualMachineCredit(newOrionGuid)).thenReturn(newCredit);
        
        
        VirtualMachine actualVm = getVmZombieResource().reviveZombieVm(testVm.vmId, newOrionGuid);
        assertEquals(testVm.vmId, actualVm.vmId);
        assertEquals(newOrionGuid, actualVm.orionGuid);
        assertEquals("+292278994-08-16T23:00:00Z", actualVm.validUntil.toString());
    }   
}
