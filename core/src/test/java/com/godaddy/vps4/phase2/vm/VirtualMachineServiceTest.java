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
        Sql.with(dataSource).exec("SELECT create_project(?, ?)", Sql.nextOrNull(rs -> rs.getLong(1)), "project4", 1);
        

        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        projectService = new JdbcProjectService(dataSource);
    }

    @Test
    public void testService() {

        Project project = projectService.createProject("My Special Project", 1);
        
        UUID orionGuid = java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");

        virtualMachineService.createVirtualMachine(orionGuid, project.getProjectId(), 1, 1, 1, 1);

        List<VirtualMachine> vms = virtualMachineService.listVirtualMachines(project.getProjectId());

        assertEquals(1, vms.size());

        virtualMachineService.destroyVirtualMachine(orionGuid);

        vms = virtualMachineService.listVirtualMachines(project.getProjectId());
        assertEquals(0, vms.size());
    }

}
