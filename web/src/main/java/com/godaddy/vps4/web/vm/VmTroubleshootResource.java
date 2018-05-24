package com.godaddy.vps4.web.vm;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.vm.TroubleshootInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.util.TroubleshootVmHelper;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmTroubleshootResource {

    private final VmResource vmResource;
    private final TroubleshootVmHelper troubleVmHelper;
    private static final Logger troubleshootLogger = LoggerFactory.getLogger(VmTroubleshootResource.class);

    @Inject
    public VmTroubleshootResource(VmResource vmResource,
            TroubleshootVmHelper troubleVmHelper) {
        this.vmResource = vmResource;
        this.troubleVmHelper = troubleVmHelper;
    }

    @GET
    @Path("/{vmId}/troubleshoot")
    public TroubleshootInfo troubleshootVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        String ip = virtualMachine.primaryIpAddress.ipAddress;

        TroubleshootInfo info = new TroubleshootInfo();
        info.status.canPing = troubleVmHelper.canPingVm(ip);
        info.status.isPortOpen2223 = troubleVmHelper.isPortOpenOnVm(ip, 2223);
        info.status.isPortOpen2224 = troubleVmHelper.isPortOpenOnVm(ip, 2224);

        if (!info.isOk()) {
            troubleshootLogger.warn("Vm " + vmId + " troubleshooting status: " + info.toString());
        }

        return info;
    }
}
