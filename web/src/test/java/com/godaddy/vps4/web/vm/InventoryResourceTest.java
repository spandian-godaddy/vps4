package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.ServiceUnavailableException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.vm.HfsInventoryData;
import com.godaddy.hfs.vm.HfsInventoryDataWrapper;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachineService;

@RunWith(MockitoJUnitRunner.class)
public class InventoryResourceTest {
    @Mock private Cache<String, HfsInventoryDataWrapper> cache;
    @Mock private CacheManager cacheManager;
    @Mock private VmService vmService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private ServerSpec mockServerSpec;
    private InventoryResource inventoryResource;
    private final int activeServerCount = 9999;
    private final int zombieServerCount = 22;

    @Before
    public void setUp() throws Exception {
        when(cacheManager.getCache(CacheName.OVH_INVENTORY, String.class, HfsInventoryDataWrapper.class))
                .thenReturn(cache);
        when(virtualMachineService.getSpec(anyString())).thenReturn(mockServerSpec);
        when(virtualMachineService.getActiveServerCountByTiers()).thenReturn(new HashMap<>());
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        when(virtualMachineService.getZombieServerCountByTiers()).thenReturn(new HashMap<>());
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        inventoryResource = new InventoryResource(cacheManager, vmService, virtualMachineService);
    }

    private List<HfsInventoryData> createDummyInventory(int available) throws IOException {
        String inventoryString = " { \"available\": " + available + ", \"name\": \"test.spec\", \"reserved\": 0, \"in_use\": 50, " +
                "\"hfs_in_use\": 1, \"retired\": 11 }";
        HfsInventoryData dummyHfsInventoryData = new ObjectMapper().readValue(inventoryString, HfsInventoryData.class);
        return Collections.singletonList(dummyHfsInventoryData);
    }

    private List<HfsInventoryData> createDummyInventory() throws IOException {
        return createDummyInventory(21);
    }

    private ServerSpec createDummyServerSpec() {
        ServerType serverType = new ServerType();
        serverType.serverType = ServerType.Type.DEDICATED;
        return new ServerSpec(1, "test.spec", "test.spec", 100, 2, 2, 2, Instant.now(),
                Instant.now().plus(1, ChronoUnit.DAYS), serverType, 1);
    }

    private Map<Integer, Integer> createDummyActiveServerCountByTiers() {
        Map<Integer, Integer> serverCountInTiersMap=new HashMap<Integer, Integer>();
        serverCountInTiersMap.put(100, activeServerCount);
        return serverCountInTiersMap;
    }

    private Map<Integer, Integer> createDummyZombieServerCountByTiers() {
        Map<Integer, Integer> serverCountInTiersMap=new HashMap<Integer, Integer>();
        serverCountInTiersMap.put(100, zombieServerCount);
        return serverCountInTiersMap;
    }

    @Test
    public void getInventoryForSpecAndTier() {
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("test.spec", 100);
        assertFalse(inventoryDetailsList.isEmpty());
        assertEquals(1, inventoryDetailsList.size());
    }


    @Test
    public void getInventoryForSpec() {
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("test.spec", 0);
        assertFalse(inventoryDetailsList.isEmpty());
        assertEquals(1, inventoryDetailsList.size());
    }

    @Test
    public void getInventoryForTier() {
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("", 100);
        assertFalse(inventoryDetailsList.isEmpty());
        assertEquals(1, inventoryDetailsList.size());
    }

    @Test
    public void getInventoryForAll() {
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("", 0);
        assertEquals(1, inventoryDetailsList.size());
        InventoryDetails inventoryDetails = inventoryDetailsList.get(0);
        assertEquals(0, inventoryDetails.vps4Active);
        assertEquals(0, inventoryDetails.vps4Zombie);
        assertEquals(1, inventoryDetails.hfsInUse);
        assertEquals(11, inventoryDetails.retired);
    }

    @Test
    public void returnEmptyListIfNoMatches() {
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("wrong.spec", 0);
        assertTrue(inventoryDetailsList.isEmpty());
    }

    @Test(expected = ServiceUnavailableException.class)
    public void throwsExceptionWhenHfsCallFails() {
        when(vmService.getInventory(anyString())).thenReturn(new ArrayList<>());
        inventoryResource.getInventory("test.spec", 0);
        fail("Expected exception to be thrown.");
    }

    @Test
    public void testGetInventoryUsesServerCountByTiersData() throws IOException{
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        when(virtualMachineService.getActiveServerCountByTiers()).thenReturn(createDummyActiveServerCountByTiers());
        when(virtualMachineService.getZombieServerCountByTiers()).thenReturn(createDummyZombieServerCountByTiers());
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("test.spec", 100);
        InventoryDetails inventoryDetails = inventoryDetailsList.get(0);
        assertEquals(activeServerCount, inventoryDetails.vps4Active);
        assertEquals(zombieServerCount, inventoryDetails.vps4Zombie);
    }

    @Test
    public void getsHfsInventoryFromCacheWhenPossible() throws IOException {
        List<HfsInventoryData> cachedData = createDummyInventory(121523);
        HfsInventoryDataWrapper cachedWrapper = new HfsInventoryDataWrapper(cachedData);
        when(cache.containsKey(CacheName.OVH_INVENTORY)).thenReturn(true);
        when(cache.get(CacheName.OVH_INVENTORY)).thenReturn(cachedWrapper);

        List<InventoryDetails> result = inventoryResource.getInventory(null, 0);
        assertEquals(121523, result.get(0).available);

        verify(cacheManager).getCache(CacheName.OVH_INVENTORY, String.class, HfsInventoryDataWrapper.class);
        verify(cache).containsKey(CacheName.OVH_INVENTORY);
        verify(cache).get(CacheName.OVH_INVENTORY);
        verify(vmService, never()).getInventory(anyString());
    }

    @Test
    public void getsHfsInventoryFromCache() throws IOException {
        when(cache.containsKey(CacheName.OVH_INVENTORY)).thenReturn(false);

        List<InventoryDetails> result = inventoryResource.getInventory(null, 0);
        assertEquals(21, result.get(0).available);

        verify(cacheManager).getCache(CacheName.OVH_INVENTORY, String.class, HfsInventoryDataWrapper.class);
        verify(cache).containsKey(CacheName.OVH_INVENTORY);
        verify(vmService).getInventory("ovhbridge");
        verify(cache, never()).get(anyString());
    }
}