package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;
import org.junit.Test;

import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;

public class VmAlertResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private VmAlertService vmAlertService = mock(VmAlertService.class);
    private VmAlertResource resource = new VmAlertResource(vmResource, vmAlertService);
    private UUID vmId = UUID.randomUUID();
    private VmMetric metric = VmMetric.CPU;
    private VirtualMachine testVm = new VirtualMachine();

    @Test
    public void getAlertList() {
        testVm.vmId = vmId;
        testVm.canceled = Instant.now().plus(7,ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;

        Image testImage = new Image();
        testImage.operatingSystem = Image.OperatingSystem.LINUX;
        testVm.image = testImage;

        when(vmResource.getVm(vmId)).thenReturn(testVm);
        resource.getMetricAlertList(vmId);
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).getVmMetricAlertList(vmId);
    }

    @Test
    public void getAlertListWindows() { // Prevent Windows from listing SSH.
        testVm.vmId = vmId;
        testVm.canceled = Instant.now().plus(7,ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;

        Image testImage = new Image();
        testImage.operatingSystem = Image.OperatingSystem.WINDOWS;
        testVm.image = testImage;
        when(vmResource.getVm(vmId)).thenReturn(testVm);
        List<VmMetricAlert> testMetricList = new ArrayList<>();

        VmMetricAlert metricFTP = new VmMetricAlert();
        metricFTP.metric= VmMetric.FTP;
        VmMetricAlert metricSSH = new VmMetricAlert();
        metricSSH.metric= VmMetric.SSH;

        testMetricList.add(metricSSH);
        testMetricList.add(metricFTP);

        when(vmAlertService.getVmMetricAlertList(vmId)).thenReturn(testMetricList);
        List<VmMetricAlert> returnedList = resource.getMetricAlertList(vmId);
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).getVmMetricAlertList(vmId);
        assertEquals(1, returnedList.size());
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
