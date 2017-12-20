package com.godaddy.vps4.phase2.monitors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.monitors.MonitorService;
import com.godaddy.vps4.monitors.jdbc.JdbcMonitorService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MonitorServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    MonitorService provisioningMonitorService = new JdbcMonitorService(dataSource);

    private UUID orionGuid = UUID.randomUUID();
    private VirtualMachine vm1, vm2, vm3, vm4, vm5;
    private Vps4User vps4User;
    private Vps4UserService vps4UserService;

    @Before
    public void setupService() {
        vps4UserService = new JdbcVps4UserService(dataSource);
        vps4User = vps4UserService.getOrCreateUserForShopper("FakeShopper");
        vm1 = SqlTestData.insertTestVm(orionGuid, dataSource);
        createActionWithDate(vm1.vmId, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, Timestamp.from(Instant.now().minus(Duration.ofMinutes(60))),vps4User.getId(), dataSource);
        vm2 = SqlTestData.insertTestVm(orionGuid, dataSource);
        createActionWithDate(vm2.vmId, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, Timestamp.from(Instant.now().minus(Duration.ofMinutes(10))),vps4User.getId(), dataSource);
        vm3 = SqlTestData.insertTestVm(orionGuid, dataSource);
        createActionWithDate(vm3.vmId, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, Timestamp.from(Instant.now().minus(Duration.ofMinutes(90))),vps4User.getId(), dataSource);
        vm4 = SqlTestData.insertTestVm(orionGuid, dataSource);
        createActionWithDate(vm4.vmId, ActionType.ENABLE_ADMIN_ACCESS, ActionStatus.IN_PROGRESS, Timestamp.from(Instant.now().minus(Duration.ofMinutes(90))),vps4User.getId(), dataSource);
        vm5 = SqlTestData.insertTestVm(orionGuid, dataSource);
        createActionWithDate(vm5.vmId, ActionType.CREATE_VM, ActionStatus.ERROR, Timestamp.from(Instant.now().minus(Duration.ofMinutes(90))),vps4User.getId(), dataSource);
    }

    @After
    public void cleanupService() {
        SqlTestData.cleanupTestVmAndRelatedData(vm1.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm2.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm3.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm4.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm5.vmId, dataSource);
        SqlTestData.deleteVps4User(vps4User.getId(), dataSource);
    }

    private static void createActionWithDate(UUID vmId, ActionType actionType, ActionStatus status, Timestamp created, long userId, DataSource dataSource){
        Sql.with(dataSource).exec("INSERT INTO vm_action (vm_id, action_type_id, status_id, created, vps4_user_id) VALUES (?, ?, ?, ?, ?)",
                null, vmId, actionType.getActionTypeId(), status.getStatusId(), created, userId);
    }

    @Test
    public void testGetVmsByActions() {
        List<UUID> problemVms = provisioningMonitorService.getVmsByActions(ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, 60);
        assertNotNull(problemVms);
        assertTrue(String.format("Expected count of problem VM's does not match actual count of {%s} VM's.", problemVms.size()), problemVms.size() == 2);
        assertTrue("Expected vm id not present in list of problem VM's.", problemVms.contains(vm1.vmId));
        assertTrue("Expected vm id not present in list of problem VM's.", problemVms.contains(vm3.vmId));
    }

}
