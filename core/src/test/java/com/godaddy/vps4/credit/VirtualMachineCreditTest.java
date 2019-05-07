package com.godaddy.vps4.credit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.godaddy.vps4.vm.DataCenterService;

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
    public void isHeritageSelfManagedIfPurchasedAtNotSetWithoutControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertTrue(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfPurchasedAtNotSetWithoutControlPanelFullyManaged() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "2"))
                .build();
        assertFalse(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfAccountPurchasedBeforeCutOverDateWithoutControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertTrue(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfPurchasedAtNotSetWithControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("CPANEL", "0"))
                .build();
        assertFalse(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfAccountPurchasedBeforeCutOverDateWithControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("PLESK", "0"))
                .build();
        assertFalse(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfAccountPurchasedAfterCutOverDateWithoutControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertFalse(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfAccountPurchasedAfterCutOverDateWithControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("PLESK", "0"))
                .build();
        assertFalse(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageSelfManagedIfAccountPurchasedBeforeCutOverDateWithoutControlPanelFullyManaged() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("MYH", "2"))
                .build();
        assertFalse(credit.isHeritageSelfManaged());
    }

    @Test
    public void isHeritageManagedIfPurchasedAtNotSetWithoutControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertFalse(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfPurchasedAtNotSetWithoutControlPanelFullyManaged() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("MYH", "2"))
                .build();
        assertFalse(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfPurchasedAtNotSetWithControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withPlanFeatures(planFeatures("CPANEL", "0"))
                .build();
        assertTrue(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfAccountPurchasedBeforeCutOverDateWithControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("CPANEL", "0"))
                .build();
        assertTrue(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfAccountPurchasedBeforeCutOverDateWithoutControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertFalse(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfAccountPurchasedAfterCutOverDateWithoutControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("MYH", "0"))
                .build();
        assertFalse(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfAccountPurchasedAfterCutOverDateWithControlPanel() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MAX))
                .withPlanFeatures(planFeatures("PLESK", "0"))
                .build();
        assertFalse(credit.isHeritageManaged());
    }

    @Test
    public void isHeritageManagedIfAccountPurchasedBeforeCutOverDateWithControlPanelFullyManaged() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta(Instant.MIN))
                .withPlanFeatures(planFeatures("CPANEL", "2"))
                .build();
        assertFalse(credit.isHeritageManaged());
    }

}