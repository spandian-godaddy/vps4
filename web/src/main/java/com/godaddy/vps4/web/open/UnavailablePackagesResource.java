package com.godaddy.vps4.web.open;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.productPackage.PackageService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.InventoryResource;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"open"})
@RequiresRole(roles = {})
@Path("/open/ded/unavailablePackages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UnavailablePackagesResource {
    private final InventoryResource inventoryResource;
    private final PackageService packageService;

    @Inject
    public UnavailablePackagesResource(InventoryResource inventoryResource, PackageService packageService) {
        this.inventoryResource = inventoryResource;
        this.packageService = packageService;
    }

    @GET
    @Path("/")
    public Set<String> getUnavailablePackages() {
        Integer[] unavailableTiers = inventoryResource
                .getInventory(null, 0)
                .stream()
                .filter(i -> i.available == 0)
                .map(i -> i.tier)
                .toArray(Integer[]::new);
        return packageService.getPackages(unavailableTiers);
    }
}
