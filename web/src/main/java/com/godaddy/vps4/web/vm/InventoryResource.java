package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.HfsInventoryData;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.RequiresRole;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private final Logger logger = LoggerFactory.getLogger(InventoryResource.class);
    private final VmService vmService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public InventoryResource(VmService vmService, VirtualMachineService virtualMachineService) {
        this.vmService = vmService;
        this.virtualMachineService = virtualMachineService;
    }

    @RequiresRole(roles = {Role.ADMIN, Role.HS_LEAD, Role.HS_AGENT, Role.SUSPEND_AUTH, Role.C3_OTHER})
    @GET
    @Path("/ded/inventory")
    @ApiOperation(value = "Get the inventory details for the specified spec or tier, otherwise return inventory on " +
            "all dedicated specs.",
            notes = "Get the inventory details for the specified spec or tier, otherwise return inventory on all " +
                    "dedicated specs.")
    public List<InventoryDetails> getInventory(@QueryParam("specName") String specName, @QueryParam("tier") int tier) {
        List<InventoryDetails> inventoryDetails = getAllInventoryDetails();

        if (!StringUtils.isBlank(specName)) {
            inventoryDetails= inventoryDetails.stream()
                    .filter(inventoryDetail -> StringUtils.equalsIgnoreCase(inventoryDetail.flavor, specName))
                    .collect(Collectors.toList());
        }
        if (tier > 0) {
            return inventoryDetails.stream().filter(inventoryDetail -> inventoryDetail.tier == tier)
                    .collect(Collectors.toList());
        }
        return inventoryDetails;
    }

    private List<InventoryDetails> getAllInventoryDetails() {
        logger.info("Invoking the call to HFS to get DED4 inventory");
        List<HfsInventoryData> hfsInventoryDataList = vmService.getInventory("ovhbridge"); // this value is hard coded for OVH.
        if(hfsInventoryDataList.isEmpty()) {
            throw new ServiceUnavailableException("Could not get inventory details from HFS.");
        }

        logger.info("Invoking the call to VPS4 to get usage data from VPS4 database");
        Map<Integer, Integer> activeServerCountByTiers = virtualMachineService.getActiveServerCountByTiers();
        Map<Integer, Integer> zombieServerCountByTiers = virtualMachineService.getZombieServerCountByTiers();

        List<InventoryDetails> inventoryDetails = new ArrayList<>();
        hfsInventoryDataList.forEach(inventoryData -> {
            ServerSpec serverSpec = virtualMachineService.getSpec(inventoryData.getName());

            if (serverSpec != null) {
                inventoryDetails.add(mapInventoryDataToDetails(inventoryData, serverSpec, activeServerCountByTiers,
                        zombieServerCountByTiers));
            } else {
                logger.info("No corresponding server spec entry found in vps4 for spec: {}",
                        inventoryData.getName());
            }
        });

        return inventoryDetails;
    }

    private InventoryDetails mapInventoryDataToDetails(HfsInventoryData matchedHfsInventoryData, ServerSpec serverSpec,
                                                       Map<Integer, Integer> activeServerCountByTiers,
                                                       Map<Integer, Integer> zombieServerCountByTiers) {
        InventoryDetails inventoryDetails = new InventoryDetails();
        inventoryDetails.flavor = matchedHfsInventoryData.getName();
        inventoryDetails.tier = serverSpec.tier;
        inventoryDetails.available = matchedHfsInventoryData.getAvailable();
        inventoryDetails.inUse = matchedHfsInventoryData.getInUse();
        inventoryDetails.hfsInUse = matchedHfsInventoryData.getHfsInUse();
        inventoryDetails.reserved = matchedHfsInventoryData.getReserved();
        inventoryDetails.cpus = serverSpec.cpuCoreCount;
        inventoryDetails.diskSize = serverSpec.diskGib;
        inventoryDetails.ram = serverSpec.memoryMib;
        inventoryDetails.diskType =
                serverSpec.specName.contains("ssd") ? "SSD" : serverSpec.specName.contains("hdd") ? "HDD" : "";
        inventoryDetails.vps4Active = activeServerCountByTiers.getOrDefault(serverSpec.tier, 0);
        inventoryDetails.vps4Zombie = zombieServerCountByTiers.getOrDefault(serverSpec.tier, 0);
        return inventoryDetails;
    }
}
