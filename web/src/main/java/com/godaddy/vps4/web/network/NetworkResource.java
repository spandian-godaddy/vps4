package com.godaddy.vps4.web.network;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.vm.Vps4AddIpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyIpAddressAction;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NetworkResource {

    private static final Logger logger = LoggerFactory.getLogger(NetworkResource.class);

    private final NetworkService networkService;
    private final ActionService actionService;
    private final VirtualMachineService virtualMachineService;
    private final ProjectService projectService;
    private final CommandService commandService;
    private final VmResource vmResource;
    private final Config config;
	private final GDUser user;


    @Inject
    public NetworkResource(GDUser user, NetworkService networkService, ActionService actionService,
            VirtualMachineService virtualMachineService, ProjectService projectService,
            CommandService commandService, VmResource vmResource, Config config){
        this.networkService = networkService;
        this.actionService = actionService;
        this.virtualMachineService = virtualMachineService;
        this.projectService = projectService;
        this.commandService = commandService;
        this.vmResource = vmResource;
        this.config = config;
        this.user = user;
    }

    private IpAddress getIpAddressInternal(UUID vmId, long ipAddressId){
        IpAddress ipAddress = networkService.getIpAddress(ipAddressId);
        if(ipAddress == null || !ipAddress.vmId.equals(vmId) || ipAddress.validUntil.isBefore(Instant.now())){
            throw new NotFoundException();
        }
        return ipAddress;
    }

    @GET
    @Path("/{vmId}/ipAddresses/{ipAddressId}")
    public IpAddress getIpAddress(@PathParam("vmId") UUID vmId, @PathParam("ipAddressId") long ipAddressId){
        // getVm does authorization verification
        vmResource.getVm(vmId);
        return getIpAddressInternal(vmId, ipAddressId);
    }


    @GET
    @Path("/{vmId}/ipAddresses")
    public List<IpAddress> getIpAddresses(@PathParam("vmId") UUID vmId){
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        List<IpAddress> ipAddresses = networkService.getVmIpAddresses(virtualMachine.vmId);
        if (ipAddresses != null){
            ipAddresses = ipAddresses.stream().filter(ip -> ip.validUntil.isAfter(Instant.now())).collect(Collectors.toList());
        }
        return ipAddresses;
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/ipAddresses")
    public Action addIpAddress(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);

        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);

        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.ADD_IP,
                new JSONObject().toJSONString(), vps4UserId, user.getUsername());

        Project project = projectService.getProject(virtualMachine.projectId);
        String sgid = project.getVhfsSgid();

        String zone = config.get("openstack.zone", null);

        logger.info("Adding IP to VM {}", virtualMachine.vmId);
        if(virtualMachine.hfsVmId == 0){
            throw new NotFoundException("VM was not associated with hfs vm");
        }

        Vps4AddIpAddress.Request request = new Vps4AddIpAddress.Request();
        request.setActionId(actionId);
        request.virtualMachine = virtualMachine;
        request.zone = zone;
        request.sgid = sgid;

        logger.info("Adding Ip Address with request "+ request.toString());
        Commands.execute(commandService, actionService, "Vps4AddIpAddress", request);

        return actionService.getAction(actionId);
    }

    @DELETE
    @Path("/{vmId}/ipAddresses/{ipAddressId}")
    public Action destroyIpAddress(@PathParam("vmId") UUID vmId, @PathParam("ipAddressId") long ipAddressId,
            @ApiParam(value = "Force the operation to complete if the VM is not accessible to unbind the IP", defaultValue = "false", required = true) @QueryParam("forceIfVmInaccessible") boolean forceIfVmInaccessible) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);

        IpAddress ipAddress = getIpAddressInternal(vmId, ipAddressId);

        if(ipAddress.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)){
            throw new Vps4Exception("CANNOT_DESTROY_PRIMARY_IP","Cannot destroy a VM's Primary IP");
        }

        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.DESTROY_IP,
                new JSONObject().toJSONString(), vps4UserId, user.getUsername());

        Vps4DestroyIpAddressAction.Request request = new Vps4DestroyIpAddressAction.Request();
        request.ipAddressId = ipAddressId;
        request.virtualMachine = virtualMachine;
        request.setActionId(actionId);
        request.forceIfVmInaccessible = forceIfVmInaccessible;

        Commands.execute(commandService, actionService, "Vps4DestroyIpAddressAction", request);

        return actionService.getAction(actionId);
    }

}
