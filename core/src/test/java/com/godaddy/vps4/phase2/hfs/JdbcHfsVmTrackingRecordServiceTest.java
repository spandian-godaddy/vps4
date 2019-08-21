package com.godaddy.vps4.phase2.hfs;

import static com.godaddy.vps4.hfs.HfsVmTrackingRecordService.ListFilters;
import static com.godaddy.vps4.hfs.HfsVmTrackingRecordService.Status;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

public class JdbcHfsVmTrackingRecordServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    HfsVmTrackingRecordService hfsVmService = new JdbcHfsVmTrackingRecordService(dataSource);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
    VirtualMachine vmOne, vmTwo, vmThree;
    HfsVmTrackingRecord hfs11, hfs12, hfs13, hfs21, hfs22, hfs23, hfs31, hfs32, hfs33;

    @Before
    public void setup() {
        vmOne = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
        vmTwo = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
        vmThree = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vmOne.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vmTwo.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vmThree.vmId, dataSource);
    }

    private void createHfsTrackingRecords() {
        hfs11 = hfsVmService.create(1001L, vmOne.vmId, vmOne.orionGuid);
        hfs12 = hfsVmService.create(1002L, vmOne.vmId, vmOne.orionGuid);
        hfs13 = hfsVmService.create(1003L, vmOne.vmId, vmOne.orionGuid);

        hfs21 = hfsVmService.create(2001L, vmTwo.vmId, vmTwo.orionGuid);
        hfs22 = hfsVmService.create(2002L, vmTwo.vmId, vmTwo.orionGuid);
        hfs23 = hfsVmService.create(2003L, vmTwo.vmId, vmTwo.orionGuid);

        hfs31 = hfsVmService.create(3001L, vmThree.vmId, vmThree.orionGuid);
        hfs32 = hfsVmService.create(3002L, vmThree.vmId, vmThree.orionGuid);
        hfs33 = hfsVmService.create(3003L, vmThree.vmId, vmThree.orionGuid);
    }

    @Test
    public void testGetCreateHfsVm() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vmOne.vmId, vmOne.orionGuid);
        assertEquals(1001, hfsVm.hfsVmId);
        assertEquals(vmOne.vmId, hfsVm.vmId);
        assertEquals(vmOne.orionGuid, hfsVm.orionGuid);
        assertNotNull(hfsVm.requested);
        assertNull(hfsVm.created);
        assertNull(hfsVm.canceled);
        assertNull(hfsVm.destroyed);
    }

    @Test
    public void testSetCreated() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vmOne.vmId, vmOne.orionGuid);
        hfsVmService.setCreated(hfsVm.hfsVmId);
        HfsVmTrackingRecord hfsVm2 = hfsVmService.get(hfsVm.hfsVmId);
        assertNotNull(hfsVm2.created);
    }

    @Test
    public void testSetCanceled() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vmOne.vmId, vmOne.orionGuid);
        hfsVmService.setCanceled(hfsVm.hfsVmId);
        hfsVm = hfsVmService.get(hfsVm.hfsVmId);
        assertNotNull(hfsVm.canceled);
    }

    @Test
    public void testSetDestroyed() {
        HfsVmTrackingRecord hfsVm = hfsVmService.create(1001, vmOne.vmId, vmOne.orionGuid);
        hfsVmService.setDestroyed(hfsVm.hfsVmId);
        hfsVm = hfsVmService.get(hfsVm.hfsVmId);
        assertNotNull(hfsVm.destroyed);
    }

    @Test
    public void getCanceledHfsVms() {
        createHfsTrackingRecords();
        hfsVmService.setCanceled(hfs11.hfsVmId);
        hfsVmService.setCanceled(hfs21.hfsVmId);
        hfsVmService.setCanceled(hfs31.hfsVmId);

        // This entry shouldn't be included as it has also been marked as destroyed
        hfsVmService.setCanceled(hfs32.hfsVmId);
        hfsVmService.setDestroyed(hfs32.hfsVmId);

        Long[] expected = {hfs11.hfsVmId, hfs21.hfsVmId, hfs31.hfsVmId};
        Long[] notExpected = {hfs12.hfsVmId, hfs13.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs32.hfsVmId, hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.byStatus = Status.CANCELED;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }

    @Test
    public void getUnusedHfsVms() {
        createHfsTrackingRecords();
        hfsVmService.setCreated(hfs11.hfsVmId);
        hfsVmService.setCreated(hfs12.hfsVmId);

        // This hfs vm is now the official vm associated with the vps4 vm
        hfsVmService.setCreated(hfs13.hfsVmId);
        virtualMachineService.addHfsVmIdToVirtualMachine(vmOne.vmId, hfs13.hfsVmId);

        Long[] expected = {hfs11.hfsVmId, hfs12.hfsVmId};
        Long[] notExpected = {hfs13.hfsVmId, hfs21.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs31.hfsVmId, hfs32.hfsVmId,
                hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.byStatus = Status.UNUSED;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }

    @Test
    public void getRequestedHfsVms() {
        createHfsTrackingRecords();
        hfsVmService.setCreated(hfs11.hfsVmId);
        hfsVmService.setCreated(hfs21.hfsVmId);
        hfsVmService.setCreated(hfs31.hfsVmId);

        Long[] notExpected = {hfs11.hfsVmId, hfs21.hfsVmId, hfs31.hfsVmId};
        Long[] expected = {hfs12.hfsVmId, hfs13.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs32.hfsVmId, hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.byStatus = Status.REQUESTED;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }

    @Test
    public void getVmsRelatedToVps4Vm() {
        createHfsTrackingRecords();

        Long[] expected = {hfs11.hfsVmId, hfs12.hfsVmId, hfs13.hfsVmId};
        Long[] notExpected = {hfs21.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs31.hfsVmId, hfs32.hfsVmId, hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.vmId = vmOne.vmId;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }

    @Test
    public void getVmsRelatedToHfsVm() {
        createHfsTrackingRecords();

        Long[] expected = {hfs11.hfsVmId};
        Long[] notExpected = {hfs12.hfsVmId, hfs13.hfsVmId, hfs21.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs31.hfsVmId,
                hfs32.hfsVmId, hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.hfsVmId = hfs11.hfsVmId;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }

    @Test
    public void getVmsByStatusAndVps4Vm() {
        createHfsTrackingRecords();

        hfsVmService.setCanceled(hfs11.hfsVmId);
        hfsVmService.setCanceled(hfs12.hfsVmId);
        hfsVmService.setCanceled(hfs21.hfsVmId);

        Long[] expected = {hfs11.hfsVmId, hfs12.hfsVmId};
        Long[] notExpected = {hfs13.hfsVmId, hfs21.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs31.hfsVmId, hfs32.hfsVmId,
                hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.byStatus = Status.CANCELED;
        listFilters.vmId = vmOne.vmId;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }

    @Test
    public void getVmsByStatusHfsVmIdAndVps4Vm() {
        createHfsTrackingRecords();

        hfsVmService.setCanceled(hfs11.hfsVmId);
        hfsVmService.setCanceled(hfs12.hfsVmId);
        hfsVmService.setCanceled(hfs21.hfsVmId);

        Long[] expected = {hfs11.hfsVmId};
        Long[] notExpected = {hfs12.hfsVmId, hfs13.hfsVmId, hfs21.hfsVmId, hfs22.hfsVmId, hfs23.hfsVmId, hfs31.hfsVmId,
                hfs32.hfsVmId, hfs33.hfsVmId};
        ListFilters listFilters = new ListFilters();
        listFilters.byStatus = Status.CANCELED;
        listFilters.vmId = vmOne.vmId;
        listFilters.hfsVmId = hfs11.hfsVmId;
        List<Long> actual = hfsVmService.getTrackingRecords(listFilters).stream().map(a -> a.hfsVmId).collect(
                Collectors.toList());

        assertThat(Arrays.asList(expected), everyItem(isIn(actual)));
        assertThat(Arrays.asList(notExpected), everyItem(not(isIn(actual))));
    }
}
