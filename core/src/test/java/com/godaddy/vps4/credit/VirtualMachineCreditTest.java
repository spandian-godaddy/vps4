package com.godaddy.vps4.credit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.godaddy.vps4.vm.DataCenterService;

public class VirtualMachineCreditTest {
    @Test
    public void isGrandFatheredIfPurchasedAtNotSet() throws Exception {
        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class)).build();
        assertTrue(credit.isHeritage());
    }

    @Test
    public void isGrandFatheredIfAccountPurchasedBeforeCutOverDate() throws Exception {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("purchased_at", "2019-04-01T00:00:00Z");

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta)
                .build();
        assertTrue(credit.isHeritage());
    }

    @Test
    public void isNotGrandFatheredIfAccountPurchasedAfterCutOverDate() throws Exception {
        Map<String, String> productMeta = new HashMap<>();
        productMeta.put("purchased_at", "2099-06-01T00:00:00Z");

        VirtualMachineCredit credit = new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withProductMeta(productMeta)
                .build();
        assertFalse(credit.isHeritage());
    }

}