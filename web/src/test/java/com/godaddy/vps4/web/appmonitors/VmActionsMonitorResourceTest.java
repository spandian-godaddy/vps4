package com.godaddy.vps4.web.appmonitors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        SnapshotActionData snapshotActionData = new SnapshotActionData("fake-action-id-1", UUID.randomUUID(), UUID.randomUUID());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-2", UUID.randomUUID(), UUID.randomUUID());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-3", UUID.randomUUID(), UUID.randomUUID());
        expectedSnapshotActionData.add(snapshotActionData);

        provisioningMonitorResource = new VmActionsMonitorResource(monitorService);
    }

    @Test
    public void testGetProvisioningPendingVms() {
        when(monitorService.getVmsByActions(ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, 60L)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getProvisioningPendingVms(60L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetStartPendingVms() {
        when(monitorService.getVmsByActions(ActionType.START_VM, ActionStatus.IN_PROGRESS, 15L)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getStartPendingVms(15L);

        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetStopPendingVms() {
        when(monitorService.getVmsByActions(ActionType.STOP_VM, ActionStatus.IN_PROGRESS, 15L)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getStopPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetRestartPendingVms() {
        when(monitorService.getVmsByActions(ActionType.RESTART_VM, ActionStatus.IN_PROGRESS, 15L)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getRestartPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetBackupPendingActions() {
        when(monitorService.getVmsBySnapshotActions(ActionType.CREATE_SNAPSHOT, ActionStatus.IN_PROGRESS, 120L)).thenReturn(expectedSnapshotActionData);
        List<SnapshotActionData> actualSnapshotActionData = provisioningMonitorResource.getBackupPendingActions(120L);
        Assert.assertNotNull(actualSnapshotActionData);
        Assert.assertEquals(expectedSnapshotActionData.size(), actualSnapshotActionData.size());
    }

    @Test
    public void testGetRestorePendingVms() {
        when(monitorService.getVmsByActions(ActionType.RESTORE_VM, ActionStatus.IN_PROGRESS, 120L)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = provisioningMonitorResource.getRestorePendingVms(120L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }
}