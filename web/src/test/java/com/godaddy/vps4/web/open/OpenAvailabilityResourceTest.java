package com.godaddy.vps4.web.open;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.InventoryDetails;
import com.godaddy.vps4.web.vm.InventoryResource;

@RunWith(MockitoJUnitRunner.class)
public class OpenAvailabilityResourceTest {
    @Mock private InventoryResource inventoryResource;
    @Mock private VirtualMachineService virtualMachineService;

    private List<InventoryDetails> inventory;
    private OpenAvailabilityResource resource;

    @Before
    public void setUp() {
        setUpInventory();
        resource = new OpenAvailabilityResource(inventoryResource, virtualMachineService);
    }

    private void setUpInventory() {
        inventory = new ArrayList<>();
        inventory.add(createInventoryDetail("ded.hdd.c4.r32.d8000", 60, 10));
        inventory.add(createInventoryDetail("ded.hdd.c6.r64.d8000", 80, 0));
        inventory.add(createInventoryDetail("ded.ssd.c4.r32.d1024", 120, 20));
        when(virtualMachineService.getSpec(60, 2)).thenReturn(createServerSpec(15, ServerType.Type.VIRTUAL));
        when(virtualMachineService.getSpec(60, 2)).thenReturn(createServerSpec(60, ServerType.Type.DEDICATED));
        when(virtualMachineService.getSpec(80, 2)).thenReturn(createServerSpec(80, ServerType.Type.DEDICATED));
        when(virtualMachineService.getSpec(120, 2)).thenReturn(createServerSpec(120, ServerType.Type.DEDICATED));
        when(inventoryResource.getInventory(eq(null), anyInt())).thenReturn(inventory);
    }

    private InventoryDetails createInventoryDetail(String flavor, int tier, int available) {
        InventoryDetails detail = new InventoryDetails();
        detail.flavor = flavor;
        detail.tier = tier;
        detail.available = available;
        return detail;
    }

    private ServerSpec createServerSpec(int tier, ServerType.Type type) {
        ServerSpec serverSpec = new ServerSpec();
        serverSpec.serverType = new ServerType();
        serverSpec.serverType.serverType = type;
        serverSpec.tier = tier;
        return serverSpec;
    }

    @Test
    public void requiresNoRoles() {
        Class<OpenAvailabilityResource> resourceClass = OpenAvailabilityResource.class;
        assertEquals(0, resourceClass.getAnnotation(RequiresRole.class).roles().length);
    }

    @Test
    public void getsAvailability() {
        Map<String, Boolean> response = resource.getAvailability();
        verify(inventoryResource, times(1)).getInventory(null, 0);
        assertEquals(3, response.size());
        assertEquals(true, response.get(inventory.get(0).flavor));
        assertEquals(false, response.get(inventory.get(1).flavor));
        assertEquals(true, response.get(inventory.get(2).flavor));
    }

    @Test
    public void getsAvailabilityForTier() {
        assertTrue(resource.getTierAvailability(60).available);
        assertFalse(resource.getTierAvailability(80).available);
        assertTrue(resource.getTierAvailability(120).available);

        verify(inventoryResource, times(3)).getInventory(eq(null), anyInt());
        verify(inventoryResource, times(1)).getInventory(null, 60);
        verify(inventoryResource, times(1)).getInventory(null, 80);
        verify(inventoryResource, times(1)).getInventory(null, 120);

        verify(virtualMachineService, times(3)).getSpec(anyInt(), anyInt());
        verify(virtualMachineService, times(1)).getSpec(60, 2);
        verify(virtualMachineService, times(1)).getSpec(80, 2);
        verify(virtualMachineService, times(1)).getSpec(120, 2);
    }

    @Test
    public void getAvailabilityForInvalidTier() {
        try {
            resource.getTierAvailability(999);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_TIER", e.getId());
            assertEquals("Tier is not a valid DED4 tier", e.getMessage());
        }
    }

    @Test
    public void getAvailabilityForVirtualTier() {
        try {
            resource.getTierAvailability(15);
        } catch (Vps4Exception e) {
            assertEquals("INVALID_TIER", e.getId());
            assertEquals("Tier is not a valid DED4 tier", e.getMessage());
        }
    }
}
