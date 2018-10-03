package com.godaddy.vps4.phase2.appmonitors.jdbc;

import com.godaddy.vps4.appmonitors.MonitoringCheckpoint;
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
    public void setupTests() throws SQLException {
        if (dataSource == null) {
            dataSource = injector.getInstance(DataSource.class);
        }
        jdbcMonitorService = new JdbcMonitorService(dataSource);
    }

    @Test
    public void testUpsertCheckpoint() {
        jdbcMonitorService.deleteMonitoringCheckpoint(ActionType.CREATE_VM);

        MonitoringCheckpoint nullCheckpoint = jdbcMonitorService.getMonitoringCheckpoint(ActionType.CREATE_VM);
        Assert.assertNull(nullCheckpoint);

        MonitoringCheckpoint initialCheckpoint = jdbcMonitorService.setMonitoringCheckpoint(ActionType.CREATE_VM);
        Assert.assertNotNull(initialCheckpoint);

        MonitoringCheckpoint testCheckpoint = jdbcMonitorService.setMonitoringCheckpoint(ActionType.CREATE_VM);
        Assert.assertTrue("New checkpoint is not after the initial checkpoint",
                testCheckpoint.checkpoint.isAfter(initialCheckpoint.checkpoint));

        MonitoringCheckpoint sameCheckpoint = jdbcMonitorService.getMonitoringCheckpoint(ActionType.CREATE_VM);
        Assert.assertTrue(testCheckpoint.checkpoint.equals(sameCheckpoint.checkpoint));

        jdbcMonitorService.deleteMonitoringCheckpoint(ActionType.CREATE_VM);
    }

    @Test
    public void testGetCheckpoints() {
        jdbcMonitorService.setMonitoringCheckpoint(ActionType.START_VM);
        jdbcMonitorService.setMonitoringCheckpoint(ActionType.STOP_VM);

        List<MonitoringCheckpoint> checkpoints = jdbcMonitorService.getMonitoringCheckpoints();
        Assert.assertTrue(checkpoints.size() >= 2);

        jdbcMonitorService.deleteMonitoringCheckpoint(ActionType.START_VM);
        jdbcMonitorService.deleteMonitoringCheckpoint(ActionType.STOP_VM);
    }
}
