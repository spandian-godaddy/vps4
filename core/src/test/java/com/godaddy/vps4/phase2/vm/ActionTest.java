package com.godaddy.vps4.phase2.vm;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4User;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ActionTest {
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    ActionService actionService = new JdbcVmActionService(dataSource);

    private UUID orionGuid = UUID.randomUUID();
    Vps4User vps4User;
    ActionType type;
    VirtualMachine vm;

    @Before
    public void setupService(){
        vps4User = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        type = ActionType.CREATE_VM;
    }

    @After
    public void cleanupService() {
        Sql.with(dataSource).exec("DELETE from vm_action where vm_id = ?", null, vm.vmId);
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.deleteTestVps4User(dataSource);
    }

    @Test
    public void testCreate(){
        long actionId = actionService.createAction(vm.vmId, type, new JSONObject().toJSONString(), "tester");
        Action action = actionService.getAction(actionId);
        assertEquals("{}", action.request);
        assertEquals(vm.vmId, action.resourceId);
        assertTrue(action.type == ActionType.CREATE_VM);
        assertEquals(ActionStatus.NEW, action.status);
        assertEquals(ActionType.CREATE_VM, action.type);
    }

    @Test
    public void testUpdateStatus(){
        long actionId = actionService.createAction(vm.vmId, type, new JSONObject().toJSONString(), "tester");
        Action action = actionService.getAction(actionId);
        assertEquals(ActionStatus.NEW, action.status);

        actionService.completeAction(actionId, "{ \"some\": \"response\" }", "some notes");

        action = actionService.getAction(action.id);
        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals("{ \"some\": \"response\" }", action.response);
        assertEquals("some notes", action.note);
    }
}
