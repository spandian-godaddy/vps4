package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.vm.VmOutageService;
import com.godaddy.vps4.vm.jdbc.JdbcVmOutageService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcVmOutageServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    private VmOutageService vmOutageService;
    private VirtualMachine vm;
    private UUID orionGuid = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        vmOutageService = new JdbcVmOutageService(dataSource);
    }

    @After
    public void tearDown() throws Exception {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void canCreateAndGetOutage() {
        Instant expectedStart = Instant.now();
        int outageId = vmOutageService.newVmOutage(vm.vmId, VmMetric.DISK, expectedStart, "disk is pretty full", 1234L);
        VmOutage newOutage = vmOutageService.getVmOutage(outageId);
        assertEquals("DISK", newOutage.metric.name());
        assertEquals(expectedStart, newOutage.started);
        assertEquals(null, newOutage.ended);
        assertEquals("disk is pretty full", newOutage.reason);
        assertEquals(1234L, newOutage.outageDetailId);
    }

    @Test
    public void canClearOutage() {
        int outageId = vmOutageService.newVmOutage(vm.vmId, VmMetric.DISK, Instant.now(), "disk is pretty full", 1234L);
        Instant expectedEnd = Instant.now().plus(Duration.ofMinutes(10));
        vmOutageService.clearVmOutage(outageId, expectedEnd);
        VmOutage newOutage = vmOutageService.getVmOutage(outageId);
        assertEquals(expectedEnd, newOutage.ended);
    }

    @Test
    public void getOutageList() {
        List<VmOutage> outageList = vmOutageService.getVmOutageList(vm.vmId);
        assertEquals(0, outageList.size());
        vmOutageService.newVmOutage(vm.vmId, VmMetric.DISK, Instant.now(), "disk is pretty full", 1234L);
        outageList = vmOutageService.getVmOutageList(vm.vmId);
        assertEquals(1, outageList.size());
    }

    @Test
    public void getOutageListByMetric() {
        vmOutageService.newVmOutage(vm.vmId, VmMetric.DISK, Instant.now(), "disk is pretty full", 1234L);
        List<VmOutage> outageList = vmOutageService.getVmOutageList(vm.vmId, VmMetric.CPU);
        assertEquals(0, outageList.size());
        outageList = vmOutageService.getVmOutageList(vm.vmId, VmMetric.DISK);
        assertEquals(1, outageList.size());
    }

}
