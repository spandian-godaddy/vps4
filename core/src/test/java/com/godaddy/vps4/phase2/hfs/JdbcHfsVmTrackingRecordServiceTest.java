package com.godaddy.vps4.phase2.hfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.hfs.HfsVmTrackingRecord;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.hfs.jdbc.JdbcHfsVmTrackingRecordService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdbcHfsVmTrackingRecordServiceTest {
    
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    HfsVmTrackingRecordService hfsVmService = new JdbcHfsVmTrackingRecordService(dataSource);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
    VirtualMachine vm;

    @Before
    public void setup() {
        vm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
    }

    @Test
    public void testGetCreateHfsVm() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        assertEquals(1001, hfsVm.hfsVmId);
        assertEquals(vm.vmId, hfsVm.vmId);
        assertEquals(vm.orionGuid, hfsVm.orionGuid);
        assertNotNull(hfsVm.requested);
        assertNull(hfsVm.created);
        assertNull(hfsVm.canceled);
        assertNull(hfsVm.destroyed);
    }

    @Test
    public void testSetCreated() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        hfsVmService.setCreated(hfsVm.hfsVmId);
        HfsVmTrackingRecord hfsVm2 = hfsVmService.get(hfsVm.hfsVmId);
        assertNotNull(hfsVm2.created);
    }

    @Test
    public void testSetCanceled() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        hfsVmService.setCanceled(hfsVm.hfsVmId);
        hfsVm = hfsVmService.get(hfsVm.hfsVmId);
        assertNotNull(hfsVm.canceled);
    }

    @Test
    public void testSetDestroyed() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        hfsVmService.setDestroyed(hfsVm.hfsVmId);
        hfsVm = hfsVmService.get(hfsVm.hfsVmId);
        assertNotNull(hfsVm.destroyed);
    }

    @Test
    public void testGetCanceledHfsVms() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        HfsVmTrackingRecord hfsVm2 = hfsVmService.create(1002, vm.vmId, vm.orionGuid);
        HfsVmTrackingRecord hfsVm3 = hfsVmService.create(1003, vm.vmId, vm.orionGuid);
        hfsVmService.setCanceled(hfsVm.hfsVmId);
        hfsVmService.setCanceled(hfsVm2.hfsVmId);
        hfsVmService.setDestroyed(hfsVm2.hfsVmId);
        hfsVmService.setCreated(hfsVm3.hfsVmId);
        List<HfsVmTrackingRecord> result = hfsVmService.getCanceled();
        hfsVm = result.get(0);
        assertNotNull(hfsVm.canceled);
        assertNull(hfsVm.destroyed);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetUnusedHfsVms() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        HfsVmTrackingRecord hfsVm2 = hfsVmService.create(2002, vm.vmId, vm.orionGuid);
        HfsVmTrackingRecord hfsVm3 = hfsVmService.create(1002, vm.vmId, vm.orionGuid);
        hfsVmService.setCreated(hfsVm.hfsVmId);
        hfsVmService.setCreated(hfsVm2.hfsVmId);
        hfsVmService.setCreated(hfsVm3.hfsVmId);
        hfsVmService.setCanceled(hfsVm3.hfsVmId);
        virtualMachineService.addHfsVmIdToVirtualMachine(vm.vmId, 2002);
        List<HfsVmTrackingRecord> result = hfsVmService.getUnused();
        hfsVm = result.get(0);
        assertEquals(1, result.size());
        assertNotNull(hfsVm.requested);
    }

    @Test
    public void testGetRequestedHfsVms() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vm.vmId, vm.orionGuid);
        HfsVmTrackingRecord hfsVm2 = hfsVmService.create(1002, vm.vmId, vm.orionGuid);
        HfsVmTrackingRecord hfsVm3 = hfsVmService.create(1003, vm.vmId, vm.orionGuid);
        hfsVmService.setCreated(hfsVm2.hfsVmId);
        hfsVmService.setCanceled(hfsVm3.hfsVmId);
        List<HfsVmTrackingRecord> result = hfsVmService.getRequested();
        hfsVm = result.get(0);
        assertNull(hfsVm.canceled);
        assertNull(hfsVm.destroyed);
        assertEquals(1, result.size());
    }

}
