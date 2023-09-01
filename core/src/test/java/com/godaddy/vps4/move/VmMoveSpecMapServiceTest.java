package com.godaddy.vps4.move;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.move.jdbc.JdbcVmMoveSpecMapService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VmMoveSpecMapServiceTest {

    private VmMoveSpecMapService vmMoveSpecMapService;
    private VirtualMachineService virtualMachineService;
    private Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        vmMoveSpecMapService = new JdbcVmMoveSpecMapService(dataSource);
        virtualMachineService = new JdbcVirtualMachineService(dataSource);
    }

    @Test
    public void testGetMap() {
        ServerSpec osSpec = virtualMachineService.getSpec("hosting.c4.r16.d200");
        ServerSpec ohSpec = virtualMachineService.getSpec("oh.hosting.c4.r16.d200");
        VmMoveSpecMap map = vmMoveSpecMapService.getVmMoveSpecMap(osSpec.specId, ohSpec.serverType.platform);
        assertEquals(ohSpec.specId, map.toSpecId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMapThrowsExceptionIfNull() {
        ServerSpec ohSpec = virtualMachineService.getSpec("oh.hosting.c4.r16.d200");
        vmMoveSpecMapService.getVmMoveSpecMap(ohSpec.specId, ohSpec.serverType.platform);
        fail("Expected exception to be thrown.");
    }
}
