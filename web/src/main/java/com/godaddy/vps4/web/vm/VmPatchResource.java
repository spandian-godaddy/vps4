package com.godaddy.vps4.web.vm;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmPatchResource {

    private static final Logger logger = LoggerFactory.getLogger(VmResource.class);
    
    final VirtualMachineService virtualMachineService;
    
    @Inject
    public VmPatchResource(VirtualMachineService virtualMachineService){
        this.virtualMachineService = virtualMachineService;
    }
    
    public static class VmPatch{
        public String name;
    }
    
    @PATCH
    @Path("/{vmId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Update VM Attributes", httpMethod = "PATCH")
    public void updateVm(@PathParam("vmId") long vmId, VmPatch vmPatch){
        Map<String, Object>  vmPatchMap = new HashMap<String,Object>();
        if(vmPatch.name != null && !vmPatch.name.equals(""))
            vmPatchMap.put("name", vmPatch.name);
        logger.info("Updating vm {}'s with {} ", vmId, vmPatchMap.toString());
        virtualMachineService.updateVirtualMachine(vmId, vmPatchMap);
    }
}
