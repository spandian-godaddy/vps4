package com.godaddy.vps4.phase2.hfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.hfs.HfsVmTrackingRecord;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.hfs.jdbc.JdbcHfsVmTrackingRecordService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdbcHfsVmServiceTest {
    
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    HfsVmTrackingRecordService hfsVmService = new JdbcHfsVmTrackingRecordService(dataSource);
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
        HfsVmTrackingRecord hfsVm = hfsVmService.createHfsVm(1001, vm.vmId, vm.orionGuid);
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
        HfsVmTrackingRecord hfsVm = hfsVmService.createHfsVm(1001, vm.vmId, vm.orionGuid);
        hfsVmService.setHfsVmCreated(hfsVm.hfsVmId);
        HfsVmTrackingRecord hfsVm2 = hfsVmService.getHfsVm(hfsVm.hfsVmId);
        assertNotNull(hfsVm2.created);
    }

    @Test
    public void testSetCanceled() {
        HfsVmTrackingRecord hfsVm = hfsVmService.createHfsVm(1001, vm.vmId, vm.orionGuid);
        hfsVmService.setHfsVmCanceled(hfsVm.hfsVmId);
        hfsVm = hfsVmService.getHfsVm(hfsVm.hfsVmId);
        assertNotNull(hfsVm.canceled);
    }

    @Test
    public void testSetDestroyed() {
        HfsVmTrackingRecord hfsVm = hfsVmService.createHfsVm(1001, vm.vmId, vm.orionGuid);
        hfsVmService.setHfsVmDestroyed(hfsVm.hfsVmId);
        hfsVm = hfsVmService.getHfsVm(hfsVm.hfsVmId);
        assertNotNull(hfsVm.destroyed);
    }

}
