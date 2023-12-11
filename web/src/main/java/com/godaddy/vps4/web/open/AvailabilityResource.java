package com.godaddy.vps4.web.open;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.InventoryResource;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"open"})
@RequiresRole(roles = {})
@Path("/open/ded/availability")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvailabilityResource {
    private final InventoryResource inventoryResource;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public AvailabilityResource(InventoryResource inventoryResource, VirtualMachineService virtualMachineService) {
        this.inventoryResource = inventoryResource;
        this.virtualMachineService = virtualMachineService;
    }

    @GET
    @Path("/")
    public Map<String, Boolean> getAvailability() {
        return inventoryResource.getInventory(null, 0)
                                .stream()
                                .collect(Collectors.toMap(kv -> kv.flavor, kv -> kv.available > 0));
    }

    @GET
    @Path("/{tier}")
    public AvailabilityTierResponse getTierAvailability(@PathParam("tier") int tier) {
        ServerSpec serverSpec = virtualMachineService.getSpec(tier, ServerType.Type.DEDICATED.getTypeId());

        if (serverSpec != null) {
            AvailabilityTierResponse response = new AvailabilityTierResponse();
            response.available = inventoryResource.getInventory(null, tier)
                                                  .stream()
                                                  .anyMatch(i -> i.tier == serverSpec.tier && i.available > 0);
            return response;
        }

        throw new Vps4Exception("INVALID_TIER", "Tier is not a valid DED4 tier");
    }

    public static class AvailabilityTierResponse {
        public boolean available;
    }
}
