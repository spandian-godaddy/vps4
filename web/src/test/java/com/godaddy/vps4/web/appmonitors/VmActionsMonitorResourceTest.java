package com.godaddy.vps4.web.appmonitors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VmActionsMonitorResourceTest {

    private VmActionsMonitorResource provisioningMonitorResource;
    private MonitorService monitorService = mock(MonitorService.class);
    private List<VmActionData> expectedVmActionData;
    private List<SnapshotActionData> expectedSnapshotActionData;

    @Before
    public void setupTest() {
        expectedVmActionData = new ArrayList<>();
        VmActionData vmActionData = new VmActionData("fake-action-id-1", UUID.randomUUID(), UUID.randomUUID());
        expectedVmActionData.add(vmActionData);

        vmActionData = new VmActionData("fake-action-id-2", UUID.randomUUID(), UUID.randomUUID());
        expectedVmActionData.add(vmActionData);

        vmActionData = new VmActionData("fake-action-id-3", UUID.randomUUID(), UUID.randomUUID());
        expectedVmActionData.add(vmActionData);

        expectedSnapshotActionData = new ArrayList<>();

        SnapshotActionData snapshotActionData = new SnapshotActionData("fake-action-id-1", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-2", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-3", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        provisioningMonitorResource = new VmActionsMonitorResource(monitorService);
    }

    @Test
    public void testGetProvisioningPendingVms() {
        when(monitorService.getVmsByActions(60L, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getProvisioningPendingVms(60L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetStartPendingVms() {
        when(monitorService.getVmsByActions(15L, ActionType.START_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getStartPendingVms(15L);

        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetStopPendingVms() {
        when(monitorService.getVmsByActions(15L, ActionType.STOP_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getStopPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetRestartPendingVms() {
        when(monitorService.getVmsByActions(15L, ActionType.RESTART_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getRestartPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetBackupPendingActions() {
        when(monitorService.getVmsBySnapshotActions(120L, ActionStatus.IN_PROGRESS, ActionStatus.NEW, ActionStatus.ERROR)).thenReturn(expectedSnapshotActionData);
        List<SnapshotActionData> actualSnapshotActionData = provisioningMonitorResource.getBackupPendingActions(120L);
        Assert.assertNotNull(actualSnapshotActionData);
        Assert.assertEquals(expectedSnapshotActionData.size(), actualSnapshotActionData.size());
    }

    @Test
    public void testGetRestorePendingVms() {
        when(monitorService.getVmsByActions(120L, ActionType.RESTORE_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getRestorePendingVms(120L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }
}