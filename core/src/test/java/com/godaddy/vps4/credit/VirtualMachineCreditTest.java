package com.godaddy.vps4.credit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.credit.VirtualMachineCredit.EffectiveManagedLevel;

public class VirtualMachineCreditTest {

    private Map<String, String> planFeatures(String controlPanel, String managedLevel){
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put("control_panel_type", controlPanel);
        planFeatures.put("managed_level", managedLevel);
        return planFeatures;
    }

    private Map<String, String> productMeta(Instant purchasedAt) {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("purchased_at", purchasedAt.toString());
        return productMeta;
    }

    @Test
    public void purchasedDateNotSetNoControlPanelManagedLevel0() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertEquals(EffectiveManagedLevel.SELF_MANAGED_V1, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedDateNotSetNoControlPanelManagedLevel1() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "1"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedDateNotSetNoControlPanelManagedLevel2() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "2"))
                .build();
        assertEquals(EffectiveManagedLevel.FULLY_MANAGED, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedDateNotSetWithControlPanelManagedLevel0() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("CPANEL", "0"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V1, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedDateNotSetWithControlPanelManagedLevel1() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("CPANEL", "1"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedDateNotSetWithControlPanelManagedLevel2() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("CPANEL", "2"))
                .build();
        assertEquals(EffectiveManagedLevel.FULLY_MANAGED, credit.effectiveManagedLevel());
    }


    @Test
    public void purchasedBeforeCutOffNoControlPanelManagedLevel0() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertEquals(EffectiveManagedLevel.SELF_MANAGED_V1, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedBeforeCutOffNoControlPanelManagedLevel1() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("MYH", "1"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedBeforeCutOffNoControlPanelManagedLevel2() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("MYH", "2"))
                .build();
        assertEquals(EffectiveManagedLevel.FULLY_MANAGED, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedBeforeCutOffWithControlPanelManagedLevel0() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("CPANEL", "0"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V1, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedBeforeCutOffWithControlPanelManagedLevel1() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("PLESK", "1"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedBeforeCutOffWithControlPanelManagedLevel2() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("CPANEL", "2"))
                .build();
        assertEquals(EffectiveManagedLevel.FULLY_MANAGED, credit.effectiveManagedLevel());
    }



    @Test
    public void purchasedAfterCutOffNoControlPanelManagedLevel0() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertEquals(EffectiveManagedLevel.SELF_MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedAfterCutOffNoControlPanelManagedLevel1() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("MYH", "1"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedAfterCutOffNoControlPanelManagedLevel2() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("MYH", "2"))
                .build();
        assertEquals(EffectiveManagedLevel.FULLY_MANAGED, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedAfterCutOffWithControlPanelManagedLevel0() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("CPANEL", "0"))
                .build();
        assertEquals(EffectiveManagedLevel.SELF_MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedAfterCutOffWithControlPanelManagedLevel1() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("PLESK", "1"))
                .build();
        assertEquals(EffectiveManagedLevel.MANAGED_V2, credit.effectiveManagedLevel());
    }

    @Test
    public void purchasedAfterCutOffWithControlPanelManagedLevel2() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("CPANEL", "2"))
                .build();
        assertEquals(EffectiveManagedLevel.FULLY_MANAGED, credit.effectiveManagedLevel());
    }



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
}