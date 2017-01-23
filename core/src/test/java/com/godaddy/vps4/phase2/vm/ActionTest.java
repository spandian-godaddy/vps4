package com.godaddy.vps4.phase2.vm;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.UUID;

import javax.sql.DataSource;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ActionTest {
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    ActionService actionService = new JdbcActionService(dataSource);
    ProjectService projectService = new JdbcProjectService(dataSource);
    VirtualMachineService vmService = new JdbcVirtualMachineService(dataSource);
    
    private UUID orionGuid = UUID.randomUUID();
    Project project;
    UUID vmId;
    ActionType type;
    VirtualMachine vm;

    @Before
    public void setupService(){
        project = SqlTestData.createProject(dataSource);
        vmId = SqlTestData.insertTestVm(orionGuid, project.getProjectId(), dataSource);
        vm = vmService.getVirtualMachine(vmId);
        type = ActionType.CREATE_VM;
    }

    @After
    public void cleanupService() {
        Sql.with(dataSource).exec("DELETE from vm_action where vm_id = ?", null, vm.vmId);
        SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        SqlTestData.cleanupTestProject(project.getProjectId(), dataSource);
    }

    @Test
    public void testCreate(){
        long actionId = actionService.createAction(vm.vmId, type, new JSONObject().toJSONString(), 1);
        Action action = actionService.getAction(actionId);
        assertEquals("{}", action.request);
        assertEquals(vm.vmId, action.virtualMachineId);
        assertTrue(action.type == ActionType.CREATE_VM);
        assertEquals(ActionStatus.NEW, action.status);
        assertEquals(ActionType.CREATE_VM, action.type);
    }

    @Test
    public void testUpdateStatus(){
        long actionId = actionService.createAction(vm.vmId, type, new JSONObject().toJSONString(), 1);
        Action action = actionService.getAction(actionId);
        assertEquals(ActionStatus.NEW, action.status);

        actionService.completeAction(actionId, "{ \"some\": \"response\" }", "some notes");

        action = actionService.getAction(action.id);
        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals("{ \"some\": \"response\" }", action.response);
        assertEquals("some notes", action.note);
    }
}
