package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class JdbcVirtualMachineTest {
    
    Injector injector = Guice.createInjector(new DatabaseModule(), 
                                             new VmModule(), 
                                             new SecurityModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    
    @Inject
    VirtualMachineService vmService;
    
    @Inject
    ProjectService projService;
    
    @Inject
    Vps4UserService userService;
    
    @Before
    public void setupTest(){
        injector.injectMembers(this);
    }
    

    UUID orionGuid = UUID.randomUUID();
    VirtualMachine virtualMachine;
    
    @After
    public void cleanup() {
        
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
        Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, orionGuid);
    }

    @Test
    public void testProvisionVmCreatesId() {
        vmService.createVirtualMachineCredit(orionGuid, "linux", "cPanel", 10, 1, "testShopperId");
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(1, 1, "testPrefix", orionGuid, "testName", 10, 1,
                "centos-7");
        virtualMachine = vmService.provisionVirtualMachine(params);
        assertNotNull(virtualMachine);
        vmService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, 1231);
        assertEquals(UUID.class, virtualMachine.vmId.getClass());
    }

}
