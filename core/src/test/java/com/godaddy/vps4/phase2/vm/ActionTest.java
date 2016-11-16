package com.godaddy.vps4.phase2.vm;

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
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class ActionTest {
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    ActionService actionService = new JdbcActionService(dataSource);
    ProjectService projectService = new JdbcProjectService(dataSource);
    private UUID orionGuid = UUID.randomUUID();
    Project project;
    long vmId;

    @Before
    public void setupService(){
        project = projectService.createProject("testVirtualMachineServiceProject", 1, 1);
        vmId = SqlTestData.insertTestVm(orionGuid, project.getProjectId(), dataSource);
    }

    @After
    public void cleanupService() {
        Sql.with(dataSource).exec("DELETE from vm_action where virtual_machine_id = ?", null, vmId);
        SqlTestData.cleanupTestVmAndRelatedData(vmId, orionGuid, dataSource);
        SqlTestData.cleanupTestProject(project.getProjectId(), dataSource);
    }

    @Test
    public void testCreate(){
        long actionId = actionService.createAction(vmId, "{}", 1);
        Action action = actionService.getAction(actionId);
        Assert.assertEquals("{}", action.request);
        Assert.assertEquals(vmId, action.virtualMachineId);
        Assert.assertEquals(ActionStatus.NEW, action.status);
        Assert.assertEquals(ActionType.CREATE_VM, action.type);
    }

    @Test
    public void testCreateWithJson(){
        long actionId = actionService.createAction(vmId,
                "{\"one\":1, \"2\":\"two\", \"three\":[\"omg\", \"array\"]}", 1);
        Action action = actionService.getAction(actionId);
        Assert.assertEquals("{\"one\":1, \"2\":\"two\", \"three\":[\"omg\", \"array\"]}", action.request);
    }

    @Test
    public void testUpdateStatus(){
        long actionId = actionService.createAction(vmId, "{}", 1);
        Action action = actionService.getAction(actionId);
        Assert.assertEquals(ActionStatus.NEW, action.status);

        actionService.completeAction(actionId, "{ \"some\": \"response\" }", "some notes");

        action = actionService.getAction(action.id);
        Assert.assertEquals(ActionStatus.COMPLETE, action.status);
        Assert.assertEquals("{ \"some\": \"response\" }", action.response);
        Assert.assertEquals("some notes", action.note);
    }
}
