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
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachineService;
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
    Project project;
    
    @After
    public void cleanup() {
        
        SqlTestData.cleanupTestVmAndRelatedData(1231, dataSource);
        Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, orionGuid);
        SqlTestData.cleanupTestProject(project.getProjectId(), dataSource);
    }

    @Test
    public void testProvisionVmCreatesId() {
        vmService.createVirtualMachineRequest(orionGuid, "linux", "cPanel", 10, 1, "testShopperId");
        project = projService.createProject("testProject", 1, 1, "testPrefix");
        UUID vmId = vmService.provisionVirtualMachine(orionGuid, "testName", project.getProjectId(), 1, 1, 1);
        vmService.addHfsVmIdToVirtualMachine(vmId, 1231);
        assertNotNull(vmId);
        assertEquals(UUID.class, vmId.getClass());
    }

}
