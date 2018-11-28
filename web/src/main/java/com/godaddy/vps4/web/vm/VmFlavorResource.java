package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.web.Vps4Api;

import com.godaddy.hfs.vm.Flavor;
import com.godaddy.hfs.vm.FlavorList;
import com.godaddy.hfs.vm.VmService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmFlavorResource {

    private static final Logger logger = LoggerFactory.getLogger(VmFlavorResource.class);

    private final VmService vmService;

    @Inject
    public VmFlavorResource(VmService vmService) {
        this.vmService = vmService;
    }

    @GET
    @Path("/vmFlavors")
    public List<Flavor> getFlavors() {

        logger.info("getting flavors from HFS...");

        FlavorList flavorList = vmService.listFlavors();
        logger.info("flavorList: {}", flavorList);
        if (flavorList != null && flavorList.results != null) {
            return flavorList.results;
        }
        return new ArrayList<>();
    }
}
