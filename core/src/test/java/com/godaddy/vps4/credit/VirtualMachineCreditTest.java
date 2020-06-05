package com.godaddy.vps4.credit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.godaddy.vps4.vm.DataCenterService;

public class VirtualMachineCreditTest {

    @Test
    public void isAbuseSuspendedFlagSet() throws Exception {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put(ECommCreditService.ProductMetaField.ABUSE_SUSPENDED_FLAG.toString(), String.valueOf(true));

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta)
                .build();
        assertTrue(credit.isAbuseSuspendedFlagSet());
    }

    @Test
    public void isBillingSuspendedFlagSet() throws Exception {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put(ECommCreditService.ProductMetaField.BILLING_SUSPENDED_FLAG.toString(), String.valueOf(true));

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta)
                .build();
        assertTrue(credit.isBillingSuspendedFlagSet());
    }

    private Map<String, String> planFeaturesFrom(String... keyThenValue) {
        HashMap<String, String> planFeatures = new HashMap<>();
        for (int i=0; i < keyThenValue.length; i=i+2) {
            planFeatures.put(keyThenValue[i], keyThenValue[i+1]);
        }
        System.out.println(planFeatures);
        return planFeatures;
    }

    @Test
    public void managedFalseForVpsWithManagedLevel0() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "10", "managed_level", "0"))
                .build();
        assertFalse(credit.isManaged());
        assertFalse(credit.isDed4());
    }

    @Test
    public void managedTrueForVpsWithManagedLevel1() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "30", "managed_level", "1"))
                .build();
        assertTrue(credit.isManaged());
        assertFalse(credit.isDed4());
    }

    @Test
    public void managedFalseForDed4WithManagedLevel1() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "60", "managed_level", "1"))
                .build();
        assertFalse(credit.isManaged());
        assertTrue(credit.isDed4());
    }

    @Test
    public void managedTrueForVpsWithManagedLevel2() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "47", "managed_level", "2"))
                .build();
        assertTrue(credit.isManaged());
        assertFalse(credit.isDed4());
    }

    @Test
    public void managedTrueForDed4WithManagedLevel2() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "60", "managed_level", "2"))
                .build();
        assertTrue(credit.isManaged());
        assertTrue(credit.isDed4());
    }

    @Test
    public void monitoringFalseForDed4WithManagedLevel1() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "60", "managed_level", "1"))
                .build();
        assertFalse(credit.hasMonitoring());
        assertFalse(credit.isManaged());
        assertTrue(credit.isDed4());
    }
    @Test
    public void monitoringFalseForDed4WithManagedLevel0() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("tier", "60", "managed_level", "0"))
                .build();
        assertFalse(credit.hasMonitoring());
        assertFalse(credit.isManaged());
        assertTrue(credit.isDed4());
    }

    @Test
    public void getMonitoringHandlesBooleanValue() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("monitoring", "true")).build();
        assertTrue(credit.hasMonitoring());
        assertEquals(1, credit.getMonitoring());

        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("monitoring", "false")).build();
        assertFalse(credit.hasMonitoring());
        assertEquals(0, credit.getMonitoring());
    }

    @Test
    public void getMonitoringHandlesIntegerValue() {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("monitoring", "1")).build();
        assertTrue(credit.hasMonitoring());
        assertEquals(1, credit.getMonitoring());

        credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeaturesFrom("monitoring", "0")).build();
        assertFalse(credit.hasMonitoring());
        assertEquals(0, credit.getMonitoring());
    }

}
