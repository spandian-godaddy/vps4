package com.godaddy.vps4.web.sysadmin;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.sysadmin.VmUsage;
import com.godaddy.vps4.sysadmin.VmUsageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsageStatsResource {

    final VmUsageService vmUsageService;

    final VirtualMachineService vmService;

    final PrivilegeService privilegeService;

    final Vps4User user;

    @Inject
    public UsageStatsResource(
            VmUsageService vmUsageService,
            VirtualMachineService vmService,
            PrivilegeService privilegeService,
            Vps4User user) {
        this.vmUsageService = vmUsageService;
        this.vmService = vmService;
        this.privilegeService = privilegeService;
        this.user = user;
    }

    @GET
    @Path("{vmId}/usage")
    public VmUsage getUsage(@PathParam("vmId") UUID vmId) {

        VirtualMachine vm = vmService.getVirtualMachine(vmId);
        if (vm == null) {
            throw new NotFoundException("Unknown vm: " + vmId);
        }

        privilegeService.requireAnyPrivilegeToVmId(user, vm.vmId);

        try {
            return vmUsageService.getUsage(vm.hfsVmId);
        } catch(java.text.ParseException e) {
            throw new Vps4Exception("BAD_USAGE_DATA", "Unable to parse usage data");
        }
    }
}
