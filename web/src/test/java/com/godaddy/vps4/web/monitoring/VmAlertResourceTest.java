package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.Privilege;
import com.godaddy.vps4.vm.DataCenterService;
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
    private CreditService creditService = mock(CreditService.class);
    private VmAlertResource resource = new VmAlertResource(vmResource, vmAlertService, creditService);
    private UUID vmId = UUID.randomUUID();
    private VmMetric metric = VmMetric.CPU;
    private VirtualMachine testVm = new VirtualMachine();
    private Map<String, String> planFeatures = new HashMap<>();

    private void setupTestVm(VirtualMachine testVm, Image.OperatingSystem operatingSystem) {
        testVm.vmId = vmId;
        testVm.orionGuid = UUID.randomUUID();
        testVm.canceled = Instant.now().plus(7,ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;

        Image testImage = new Image();
        testImage.operatingSystem = operatingSystem;
        testVm.image = testImage;
    }

    private VirtualMachineCredit setupTestCredit(String managedLevel) {
        planFeatures.put(ECommCreditService.PlanFeatures.MANAGED_LEVEL.toString(), managedLevel);

        VirtualMachineCredit testCredit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(UUID.randomUUID().toString())
                .withPlanFeatures(planFeatures)
                .build();

        return testCredit;
    }

    private void createTestMetricList(List<VmMetric> metrics) {
        // create each metric and store in a list
        List<VmMetricAlert> testMetricList = new ArrayList<>();
        for (VmMetric m : metrics) {
            VmMetricAlert vmMetricAlert = new VmMetricAlert();
            vmMetricAlert.metric= m;
            testMetricList.add(vmMetricAlert);
        }
        when(vmAlertService.getVmMetricAlertList(vmId)).thenReturn(testMetricList);
    }

    @Test
    public void getAlertList() {
        setupTestVm(testVm, Image.OperatingSystem.LINUX);
        VirtualMachineCredit testSelfManagedCredit = setupTestCredit("0");
        when(vmResource.getVm(vmId)).thenReturn(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(testSelfManagedCredit);
        resource.getMetricAlertList(vmId);
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).getVmMetricAlertList(vmId);
        verify(creditService).getVirtualMachineCredit(testVm.orionGuid);
    }

    @Test
    public void getAlertListWindows() { // Prevent Windows from listing SSH.
        setupTestVm(testVm, Image.OperatingSystem.WINDOWS);

        VirtualMachineCredit testSelfManagedCredit = setupTestCredit("0");

        when(vmResource.getVm(vmId)).thenReturn(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(testSelfManagedCredit);

        createTestMetricList(Arrays.asList(VmMetric.FTP, VmMetric.SSH));

        List<VmMetricAlert> returnedList = resource.getMetricAlertList(vmId);
        verify(vmResource).getVm(vmId);
        verify(vmAlertService).getVmMetricAlertList(vmId);
        verify(creditService).getVirtualMachineCredit(testVm.orionGuid);
        assertEquals(1, returnedList.size());
        assertEquals(VmMetric.FTP, returnedList.get(0).metric);
    }

    @Test
    public void getAlertListWithAdditionalFQDNs() { // Prevent returning HTTPs checks from additional domains
        setupTestVm(testVm, Image.OperatingSystem.LINUX);

        VirtualMachineCredit testSelfManagedCredit = setupTestCredit("0");

        when(vmResource.getVm(vmId)).thenReturn(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(testSelfManagedCredit);

        createTestMetricList(Arrays.asList(VmMetric.FTP, VmMetric.HTTPS));

        List<VmMetricAlert> returnedList = resource.getMetricAlertList(vmId);

        assertEquals(1, returnedList.size());
        assertEquals(VmMetric.FTP, returnedList.get(0).metric);
    }

    @Test
    public void getAlertListWithManagedCredit() { // Prevent returning FTP checks from managed
        setupTestVm(testVm, Image.OperatingSystem.LINUX);

        VirtualMachineCredit testManagedCredit = setupTestCredit("1");

        when(vmResource.getVm(vmId)).thenReturn(testVm);
        when(creditService.getVirtualMachineCredit(testVm.orionGuid)).thenReturn(testManagedCredit);

        createTestMetricList(Arrays.asList(VmMetric.FTP, VmMetric.SSH));

        List<VmMetricAlert> returnedList = resource.getMetricAlertList(vmId);

        assertEquals(1, returnedList.size());
        assertEquals(VmMetric.SSH, returnedList.get(0).metric);
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
