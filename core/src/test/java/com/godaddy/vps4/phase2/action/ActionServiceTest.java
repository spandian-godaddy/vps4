package com.godaddy.vps4.phase2.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ActionServiceTest {

    private ActionService actionService;
    private Vps4UserService vps4UserService;
    private Injector injector = Guice.createInjector(new DatabaseModule());
    
    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;
    private VirtualMachine vm;
    private Vps4User vps4User;
    
    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        actionService = new JdbcActionService(dataSource);
        vps4UserService = new JdbcVps4UserService(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        vps4User = vps4UserService.getOrCreateUserForShopper("FakeShopper", "1");
    }
    
    @After
    public void cleanup() {
        
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.deleteVps4User(vps4User.getId(), dataSource);
    }
    
    @Test
    public void testGetAllActionsForVmId(){
        ResultSubset<Action> actions = actionService.getActions(vm.vmId, 100, 0, null, null, null);
        assertEquals(null, actions);
        
        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", vps4User.getId());
        
        actions = actionService.getActions(vm.vmId, 100, 0, null, null, null);
        assertEquals(1, actions.results.size());
    }

    @Test
    public void testGetActionsByTypeForVmId(){
        ResultSubset<Action> actions = actionService.getActions(vm.vmId, 100, 0, null, null, null);
        assertEquals(null, actions);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", vps4User.getId());
        actionService.createAction(vm.vmId, ActionType.STOP_VM, "{}", vps4User.getId());
        actionService.createAction(vm.vmId, ActionType.START_VM, "{}", vps4User.getId());

        actions = actionService.getActions(vm.vmId, 100, 0, ActionType.CREATE_VM);
        assertEquals(1, actions.results.size());
    }

    @Test
    public void testGetActionsInDateRange(){
        SqlTestData.createActionWithDate(vm.vmId, ActionType.SET_HOSTNAME, Timestamp.from(Instant.now().minus(Duration.ofHours(12))), vps4User.getId(), dataSource);
        ResultSubset<Action> actions = actionService.getActions(vm.vmId, 100, 0, null, null, null);
        assertEquals(1, actions.results.size());
        
        // action created 1 hour before start filter
        actions = actionService.getActions(vm.vmId, 100, 0, null, Date.from(Instant.now().minus(Duration.ofHours(11))), null);
        assertEquals(null, actions);
        
        // action created 1 hour after start filter
        actions = actionService.getActions(vm.vmId, 100, 0, null, Date.from(Instant.now().minus(Duration.ofHours(13))), null);
        assertEquals(1, actions.results.size());
        
        // action created 1 hour after start filter, 1 hour before end filter
        actions = actionService.getActions(vm.vmId, 100, 0, null, Date.from(Instant.now().minus(Duration.ofHours(13))), Date.from(Instant.now().minus(Duration.ofHours(11))));
        assertEquals(1, actions.results.size());
        
        // action created  1 hour after end filter
        actions = actionService.getActions(vm.vmId, 100, 0, null, null, Date.from(Instant.now().minus(Duration.ofHours(13))));
        assertEquals(null, actions);
        
        // action created  1 hour before end filter
        actions = actionService.getActions(vm.vmId, 100, 0, null, null, Date.from(Instant.now().minus(Duration.ofHours(11))));
        assertEquals(1, actions.results.size());
    }
    
    @Test
    public void testStatusList(){
        
        SqlTestData.createActionWithDate(vm.vmId, ActionType.SET_HOSTNAME, Timestamp.from(Instant.now().minus(Duration.ofHours(12))), vps4User.getId(), dataSource);
        
        ResultSubset<Action> actions = actionService.getActions(vm.vmId, 100, 0);
        assertEquals(1, actions.results.size());
        
        Action action = actions.results.get(0);
        actionService.completeAction(action.id, null, null);
        
        actions = actionService.getActions(vm.vmId, 100, 0);
        
        List<String> statusList = new ArrayList<String>();
        statusList.add(ActionStatus.COMPLETE.toString());
        
        actions = actionService.getActions(vm.vmId, 100, 0, statusList);
        assertEquals(1, actions.results.size());
        
        statusList.remove(ActionStatus.COMPLETE.toString());
        statusList.add(ActionStatus.NEW.toString());
        
        actions = actionService.getActions(vm.vmId, 100, 0, statusList);
        assertEquals(null, actions);
    }

    @Test
    public void testCompleteActionPopulatesCompletedColumn(){
        SqlTestData.createActionWithDate(vm.vmId, ActionType.SET_HOSTNAME, Timestamp.from(Instant.now().minus(Duration.ofHours(12))), vps4User.getId(), dataSource);

        ResultSubset<Action> actions = actionService.getActions(vm.vmId, 100, 0);
        assertEquals(1, actions.results.size());
        Action testAction = actions.results.get(0);
        assertNull(testAction.completed);

        actionService.completeAction(testAction.id, "{}", "");

        actions = actionService.getActions(vm.vmId, 100, 0);
        assertEquals(1, actions.results.size());
        testAction = actions.results.get(0);
        assertNotNull(testAction.completed);
    }
    
}
