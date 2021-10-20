package com.godaddy.vps4.phase2.appmonitors.jdbc;

import com.godaddy.vps4.appmonitors.ActionCheckpoint;
import com.godaddy.vps4.appmonitors.jdbc.JdbcMonitorService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class JdbcMonitorServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource;
    JdbcMonitorService jdbcMonitorService;

    @Before
    public void setupTests() {
        if (dataSource == null) {
            dataSource = injector.getInstance(DataSource.class);
        }
        jdbcMonitorService = new JdbcMonitorService(dataSource);
    }

    @Test
    public void testUpsertCheckpoint() {
        jdbcMonitorService.deleteActionCheckpoint(ActionType.CREATE_VM);

        ActionCheckpoint nullCheckpoint = jdbcMonitorService.getActionCheckpoint(ActionType.CREATE_VM);
        Assert.assertNull(nullCheckpoint);

        ActionCheckpoint initialCheckpoint = jdbcMonitorService.setActionCheckpoint(ActionType.CREATE_VM);
        Assert.assertNotNull(initialCheckpoint);

        ActionCheckpoint testCheckpoint = jdbcMonitorService.setActionCheckpoint(ActionType.CREATE_VM);
        Assert.assertTrue("New checkpoint is not after the initial checkpoint",
                testCheckpoint.checkpoint.isAfter(initialCheckpoint.checkpoint));

        ActionCheckpoint sameCheckpoint = jdbcMonitorService.getActionCheckpoint(ActionType.CREATE_VM);
        Assert.assertTrue(testCheckpoint.checkpoint.equals(sameCheckpoint.checkpoint));

        jdbcMonitorService.deleteActionCheckpoint(ActionType.CREATE_VM);
    }

    @Test
    public void testGetCheckpoints() {
        jdbcMonitorService.setActionCheckpoint(ActionType.START_VM);
        jdbcMonitorService.setActionCheckpoint(ActionType.STOP_VM);

        List<ActionCheckpoint> checkpoints = jdbcMonitorService.getActionCheckpoints();
        Assert.assertTrue(checkpoints.size() >= 2);

        jdbcMonitorService.deleteActionCheckpoint(ActionType.START_VM);
        jdbcMonitorService.deleteActionCheckpoint(ActionType.STOP_VM);
    }
}
