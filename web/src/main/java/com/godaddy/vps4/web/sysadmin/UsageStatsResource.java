package com.godaddy.vps4.web.sysadmin;

import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.sysadmin.VmUsage;
import com.godaddy.vps4.sysadmin.VmUsageParser;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;

import gdg.hfs.vhfs.sysadmin.SysAdminService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsageStatsResource {

    final SysAdminService sysAdminService;

    final VirtualMachineService vmService;

    final PrivilegeService privilegeService;

    final Vps4User user;

    @Inject
    public UsageStatsResource(
            SysAdminService sysAdminService,
            VirtualMachineService vmService,
            PrivilegeService privilegeService,
            Vps4User user) {
        this.sysAdminService = sysAdminService;
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

        Response response = sysAdminService.usageStatsResults(vm.hfsVmId, null, null);

        // TODO HFS is responding with a 202, but it's giving us an immediate response
        //      without any background work.
        //      This may have been a holdover from the previous usage stats behavior.
        //
        if (response.getStatus() == HttpServletResponse.SC_ACCEPTED) {

            String json = response.readEntity(String.class);

            try {
                return new VmUsageParser()
                        .parse((JSONObject)new JSONParser().parse(json));

            } catch (ParseException e) {
                throw new Vps4Exception(
                        "BAD_USAGE_RESPONSE",
                        "Unable to parse usage response");
            }
        }

        return null;
    }
}
