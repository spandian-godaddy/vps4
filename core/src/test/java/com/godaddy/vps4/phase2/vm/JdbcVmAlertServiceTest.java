package com.godaddy.vps4.phase2.vm;

import static com.godaddy.vps4.vm.VmMetric.CPU;
import static com.godaddy.vps4.vm.VmMetric.PING;
import static com.godaddy.vps4.vm.VmMetric.RAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import com.godaddy.vps4.vm.VmMetricAlert.Status;
import com.godaddy.vps4.vm.jdbc.JdbcVmAlertService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcVmAlertServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    private VmAlertService vmAlertService;
    private VirtualMachine vm;
    private Vps4User user;
    private UUID orionGuid = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        user = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource, user.getId());
        vmAlertService = new JdbcVmAlertService(dataSource);
    }

    @After
    public void tearDown() throws Exception {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.deleteTestVps4User(dataSource);
    }

    @Test
    public void allAlertsEnabledByDefault() {
        List<VmMetricAlert> alerts = vmAlertService.getVmMetricAlertList(vm.vmId);
        for (VmMetricAlert alert : alerts) {
            assertSame(Status.ENABLED, alert.status);
            assertTrue(Arrays.asList(VmMetric.values()).contains(alert.metric));
        }
    }

    @Test
    public void getAlertByMetric() {
        VmMetricAlert alert = vmAlertService.getVmMetricAlert(vm.vmId, PING.name());
        assertEquals(PING, alert.metric);
        assertEquals("network_service", alert.type);
        assertSame(Status.ENABLED, alert.status);
    }

    @Test
    public void disableAlert() {
        vmAlertService.disableVmMetricAlert(vm.vmId, PING.name());
        VmMetricAlert alert = vmAlertService.getVmMetricAlert(vm.vmId, PING.name());
        assertSame(Status.DISABLED, alert.status);
    }

    @Test
    public void alreadyDisabledNoException() {
        vmAlertService.disableVmMetricAlert(vm.vmId, PING.name());
        vmAlertService.disableVmMetricAlert(vm.vmId, PING.name());
    }

    @Test
    public void reenableAlert() {
        vmAlertService.disableVmMetricAlert(vm.vmId, PING.name());
        vmAlertService.reenableVmMetricAlert(vm.vmId, PING.name());
        VmMetricAlert alert = vmAlertService.getVmMetricAlert(vm.vmId, PING.name());
        assertSame(Status.ENABLED, alert.status);
    }

    @Test
    public void disableMultipleAlertsInList() {
        List<VmMetric> alertsToDisable = Arrays.asList(PING, CPU, RAM);
        for (VmMetric metric : alertsToDisable) {
            vmAlertService.disableVmMetricAlert(vm.vmId, metric.name());
        }

        List<VmMetricAlert> alerts = vmAlertService.getVmMetricAlertList(vm.vmId);
        for (VmMetricAlert alert : alerts) {
            Status expectedStatus =
                    (alertsToDisable.contains(alert.metric)) ? Status.DISABLED : Status.ENABLED;
            assertSame(expectedStatus, alert.status);
        }
    }
}
