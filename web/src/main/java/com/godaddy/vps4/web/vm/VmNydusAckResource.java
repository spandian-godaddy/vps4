package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmNydusAckResource {

    private final VirtualMachineService virtualMachineService;
    private final VmResource vmResource;
    private final GDUser user;

    @Inject
    public VmNydusAckResource (VirtualMachineService virtualMachineService, VmResource vmResource, GDUser user) {
        this.virtualMachineService = virtualMachineService;
        this.vmResource = vmResource;
        this.user = user;
    }

    @POST
    @Path("/{vmId}/nydusAck")
    @ApiOperation(value = "Received the nydus warning acknowledgement from the customer")
    public void acknowledgeNydusWarning(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        validateUserIsShopper(user);
        if (!virtualMachine.hasNydusWarningAcked()) {
            virtualMachineService.ackNydusWarning(vmId);
        }
    }
}
