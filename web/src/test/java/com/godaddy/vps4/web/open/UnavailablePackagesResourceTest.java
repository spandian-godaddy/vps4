package com.godaddy.vps4.web.open;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.productPackage.PackageService;
import com.godaddy.vps4.web.vm.InventoryDetails;
import com.godaddy.vps4.web.vm.InventoryResource;

@RunWith(MockitoJUnitRunner.class)
public class UnavailablePackagesResourceTest {
    @Mock private InventoryResource inventoryResource;
    @Mock private PackageService packageService;

    private UnavailablePackagesResource resource;

    @Before
    public void setUp() {
        setUpInventory();
        resource = new UnavailablePackagesResource(inventoryResource, packageService);
    }

    private void setUpInventory() {
        List<InventoryDetails> inventory = new ArrayList<>();
        inventory.add(createInventoryDetail("ded.hdd.c4.r32.d8000", 60, 10));
        inventory.add(createInventoryDetail("ded.hdd.c6.r64.d8000", 80, 0));
        inventory.add(createInventoryDetail("ded.ssd.c4.r32.d1024", 120, 20));
        when(inventoryResource.getInventory(eq(null), anyInt())).thenReturn(inventory);
        when(packageService.getPackages(60)).thenReturn(Collections.singleton("fake-package-tier-60"));
        when(packageService.getPackages(80)).thenReturn(Collections.singleton("fake-package-tier-80"));
        when(packageService.getPackages(120)).thenReturn(Collections.singleton("fake-package-tier-120"));
    }

    private InventoryDetails createInventoryDetail(String flavor, int tier, int available) {
        InventoryDetails detail = new InventoryDetails();
        detail.flavor = flavor;
        detail.tier = tier;
        detail.available = available;
        return detail;
    }

    @Test
    public void getsPackages() {
        Set<String> result = resource.getUnavailablePackages();
        assertEquals(Collections.singleton("fake-package-tier-80"), result);
        verify(inventoryResource, times(1)).getInventory(null, 0);
        verify(packageService, times(1)).getPackages(80);
    }

    @Test
    public void getsPackagesWhenMultiplePackagesMatchTier() {
        Set<String> multiPackageSet = new HashSet<String>() {{
            add("fake-package-tier-80-a");
            add("fake-package-tier-80-b");
        }};
        when(packageService.getPackages(80)).thenReturn(multiPackageSet);
        Set<String> result = resource.getUnavailablePackages();
        assertEquals(multiPackageSet, result);
    }

    @Test
    public void getsPackagesWhenNoUnavailableTiers() {
        List<InventoryDetails> inventory = new ArrayList<>();
        inventory.add(createInventoryDetail("ded.hdd.c4.r32.d8000", 60, 10));
        inventory.add(createInventoryDetail("ded.hdd.c6.r64.d8000", 80, 30));
        inventory.add(createInventoryDetail("ded.ssd.c4.r32.d1024", 120, 20));
        when(inventoryResource.getInventory(eq(null), anyInt())).thenReturn(inventory);

        Set<String> result = resource.getUnavailablePackages();
        assertEquals(0, result.size());
    }
}
