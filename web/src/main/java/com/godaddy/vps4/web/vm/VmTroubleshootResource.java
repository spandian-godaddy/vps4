package com.godaddy.vps4.web.vm;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.vm.TroubleshootInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmTroubleshootResource {

    private final VmResource vmResource;
    private final TroubleshootVmService troubleshootVmService;
    private static final Logger logger = LoggerFactory.getLogger(VmTroubleshootResource.class);

    @Inject
    public VmTroubleshootResource(VmResource vmResource, TroubleshootVmService troubleshootVmService) {
        this.vmResource = vmResource;
        this.troubleshootVmService = troubleshootVmService;
    }

    @GET
    @Path("/{vmId}/troubleshoot")
    public TroubleshootInfo troubleshootVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        String ip = virtualMachine.primaryIpAddress.ipAddress;

        TroubleshootInfo info = new TroubleshootInfo();
        info.status.canPing = troubleshootVmService.canPingVm(ip);
        info.status.isPortOpen2224 = troubleshootVmService.isPortOpenOnVm(ip, 2224);
        info.status.hfsAgentStatus = troubleshootVmService.getHfsAgentStatus(virtualMachine.hfsVmId);

        if (!info.isOk()) {
            logger.warn("Vm " + vmId + " troubleshooting status: " + info.toString());
        }

        return info;
    }
}
