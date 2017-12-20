package com.godaddy.vps4.web.monitors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.monitors.MonitorService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProvisioningMonitorResourceTest {

    private ProvisioningMonitorResource provisioningMonitorResource;
    private MonitorService monitorService = mock(MonitorService.class);
    private List<UUID> randomUUUIDs;

    @Before
    public void setupTest() {
        randomUUUIDs = new ArrayList<>();
        randomUUUIDs.add(UUID.randomUUID());
        randomUUUIDs.add(UUID.randomUUID());
        randomUUUIDs.add(UUID.randomUUID());

        provisioningMonitorResource = new ProvisioningMonitorResource(monitorService);
    }

    @Test
    public void testGetProvisioningPendingVms() {
        when(monitorService.getVmsByActions(ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, 60L)).thenReturn(randomUUUIDs);
        List<UUID> actualUUIDs = provisioningMonitorResource.getProvisioningPendingVms(60L);
        Assert.assertNotNull(actualUUIDs);
        Assert.assertEquals(randomUUUIDs.size(), actualUUIDs.size());
    }
}