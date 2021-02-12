package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSyncStatusResource {

    private static final Logger logger = LoggerFactory.getLogger(VmSyncStatusResource.class);
    private final VmResource vmResource;
    private final GDUser user;
    private final ActionService actionService;
    private final CommandService commandService;

    @Inject
    public VmSyncStatusResource(GDUser user,
                                VmResource vmResource,
                                CommandService commandService,
                                ActionService actionService) {
        this.user = user;
        this.vmResource = vmResource;
        this.commandService = commandService;
        this.actionService = actionService;
    }

    @POST
    @Path("{vmId}/syncVmStatus")
    @ApiOperation(value = "sync Vm status with HFS",
            notes = "sync Vm status with HFS")
    public VmAction syncVmStatus(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);

        logger.info("triggering HFS sync with OpenStack/OH vm with hfs id {}", vm.hfsVmId);
        VmActionRequest syncRequest = new VmActionRequest();
        syncRequest.virtualMachine = vm;

        return createActionAndExecute(actionService, commandService, vmId,
                    ActionType.SYNC_STATUS, syncRequest, "Vps4SyncVmStatus", user);
    }

}
