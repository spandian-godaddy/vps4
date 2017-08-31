package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class JdbcVirtualMachineTest {

    @Inject VirtualMachineService vmService;
    @Inject ProjectService projService;
    @Inject Vps4UserService userService;

    Injector injector = Guice.createInjector(new DatabaseModule(),
                                             new VmModule(),
                                             new SecurityModule());

    DataSource dataSource = injector.getInstance(DataSource.class);
    UUID orionGuid = UUID.randomUUID();
    VirtualMachine virtualMachine;

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        virtualMachine = SqlTestData.insertTestVm(orionGuid, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
    }

    @Test
    public void testProvisionVmCreatesId() {
        assertNotNull(virtualMachine);
        assertEquals(UUID.class, virtualMachine.vmId.getClass());
    }

    @Test
    public void testProvisionVmUsesValidSpec() {
        assertTrue(virtualMachine.spec.validUntil.isAfter(Instant.now()));
    }
}
