package com.godaddy.vps4.web.sysadmin;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.sysadmin.VmUsage;
import com.godaddy.vps4.sysadmin.VmUsageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsageStatsResource {

    final VmResource vmResource;
    final VmUsageService vmUsageService;

    @Inject
    public UsageStatsResource( VmResource vmResource, VmUsageService vmUsageService) {
        this.vmResource = vmResource;
        this.vmUsageService = vmUsageService;
    }

    @GET
    @Path("{vmId}/usage")
    public VmUsage getUsage(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        try {
            return vmUsageService.getUsage(vm.hfsVmId);
        } catch(java.text.ParseException e) {
            throw new Vps4Exception("BAD_USAGE_DATA", "Unable to parse usage data");
        }
    }
}
