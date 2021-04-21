package com.godaddy.vps4.web.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ServiceUnavailableException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.vm.HfsInventoryData;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachineService;

public class InventoryResourceTest {

    private VmService vmService = mock(VmService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private ServerSpec mockServerSpec = mock(ServerSpec.class);
    private InventoryResource inventoryResource;
    private int activeServerCount = 9999;
    private int zombieServerCount = 22;

    @Before
    public void setUp() throws Exception {
        when(virtualMachineService.getSpec(anyString())).thenReturn(mockServerSpec);
        when(virtualMachineService.getActiveServerCountByTiers()).thenReturn(new HashMap<Integer, Integer>());
        when(virtualMachineService.getZombieServerCountByTiers()).thenReturn(new HashMap<Integer, Integer>());
        when(vmService.getInventory(anyString())).thenReturn(new ArrayList<>());
        inventoryResource = new InventoryResource(vmService, virtualMachineService);
    }

    private List<HfsInventoryData> createDummyInventory() throws IOException {
        String inventoryString = " { \"available\": 21, \"name\": \"test.spec\", \"reserved\": 0, \"in_use\": 50 }";
        HfsInventoryData dummyHfsInventoryData = new ObjectMapper().readValue(inventoryString, HfsInventoryData.class);
        return Collections.singletonList(dummyHfsInventoryData);
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
    public void getInventoryForSpecAndTier() throws IOException {
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("test.spec", 100);
        assertFalse(inventoryDetailsList.isEmpty());
        assertEquals(1, inventoryDetailsList.size());
    }


    @Test
    public void getInventoryForSpec() throws IOException {
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("test.spec", 0);
        assertFalse(inventoryDetailsList.isEmpty());
        assertEquals(1, inventoryDetailsList.size());
    }

    @Test
    public void getInventoryForTier() throws IOException {
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("", 100);
        assertFalse(inventoryDetailsList.isEmpty());
        assertEquals(1, inventoryDetailsList.size());
    }

    @Test
    public void getInventoryForAll() throws IOException {
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("", 0);
        assertEquals(1, inventoryDetailsList.size());
        InventoryDetails inventoryDetails = inventoryDetailsList.get(0);
        assertNotNull(inventoryDetails.hfsInUse);
        assertEquals(0, inventoryDetails.vps4Active);
        assertEquals(0, inventoryDetails.vps4Zombie);
    }

    @Test
    public void returnEmptyListIfNoMatches() throws IOException {
        when(vmService.getInventory(anyString())).thenReturn(createDummyInventory());
        when(virtualMachineService.getSpec(anyString())).thenReturn(createDummyServerSpec());
        List<InventoryDetails> inventoryDetailsList = inventoryResource.getInventory("wrong.spec", 0);
        assertTrue(inventoryDetailsList.isEmpty());
    }

    @Test(expected = ServiceUnavailableException.class)
    public void throwsExceptionWhenHfsCallFails() {
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
}