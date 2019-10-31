package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;

public class VmAlertResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private VmAlertService vmAlertService = mock(VmAlertService.class);

    private VmAlertResource resource = new VmAlertResource(vmResource, vmAlertService);
    private UUID vmId = UUID.randomUUID();
    private VmMetric metric = VmMetric.CPU;

    @Test
    public void getAlertList() {
        resource.getMetricAlertList(vmId);
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).getVmMetricAlertList(vmId);
    }

    @Test
    public void getSingleAlert() {
        resource.getMetricAlert(vmId, metric.name());
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).getVmMetricAlert(vmId, metric.name());
    }

    @Test
    public void getInvalidAlert() {
        try {
            resource.getMetricAlert(vmId, "BANDWIDTH");
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

    @Test
    public void disableAlert() {
        resource.disableMetricAlert(vmId, metric.name());
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).disableVmMetricAlert(vmId, metric.name());
        verify(vmAlertService).getVmMetricAlert(vmId, metric.name());
    }

    @Test
    public void disableInvalidAlert() {
        try {
            resource.disableMetricAlert(vmId, "BANDWIDTH");
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

    @Test
    public void enableAlert() {
        resource.enableMetricAlert(vmId, metric.name());
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).reenableVmMetricAlert(vmId, metric.name());
        verify(vmAlertService).getVmMetricAlert(vmId, metric.name());
    }

    @Test
    public void enableInvalidAlert() {
        try {
            resource.getMetricAlert(vmId, "BANDWIDTH");
            fail();
        } catch (Vps4Exception e) {
            assertEquals("INVALID_PARAMETER", e.getId());
        }
    }

}
