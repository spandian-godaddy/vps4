package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VirtualMachineServiceTest {

    VirtualMachineService virtualMachineService;

    ProjectService projectService;
    
    Injector injector = Guice.createInjector(new DatabaseModule());

    @Before
    public void setupService() {
    	DataSource dataSource = injector.getInstance(DataSource.class);

        Sql.with(dataSource).exec("TRUNCATE TABLE virtual_machine", null);

        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        projectService = new JdbcProjectService(dataSource);
    }

    @Test
    public void testService() {

        Project project = projectService.createProject("My Special Project", 1);

        long vmId = 42;

        virtualMachineService.createVirtualMachine(vmId, project.getSgid(), "tiny", "vm1");

        List<VirtualMachine> vms = virtualMachineService.listVirtualMachines(project.getSgid());

        assertEquals(1, vms.size());

        virtualMachineService.destroyVirtualMachine(42);

        vms = virtualMachineService.listVirtualMachines(project.getSgid());
        assertEquals(0, vms.size());
    }

}
