package com.godaddy.vps4.web.network;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.ipblacklist.IpBlacklistService;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ServerSpec;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.vm.Vps4AddIpAddress;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyIpAddressAction;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NetworkResource {

    private static final Logger logger = LoggerFactory.getLogger(NetworkResource.class);

    private final NetworkService networkService;
    private final ActionService actionService;
    private final ProjectService projectService;
    private final CommandService commandService;
    private final VmResource vmResource;
    private final Config config;
    private final GDUser user;
    private final IpBlacklistService ipBlacklistService;


    @Inject
    public NetworkResource(GDUser user,
                           NetworkService networkService,
                           ActionService actionService,
                           ProjectService projectService,
                           CommandService commandService,
                           VmResource vmResource,
                           Config config,
                           IpBlacklistService ipBlacklistService) {
        this.networkService = networkService;
        this.actionService = actionService;
        this.projectService = projectService;
        this.commandService = commandService;
        this.vmResource = vmResource;
        this.config = config;
        this.user = user;
        this.ipBlacklistService = ipBlacklistService;
    }

    private IpAddress getIpAddressInternal(UUID vmId, long addressId) {
        IpAddress ipAddress = networkService.getIpAddress(addressId);
        if (ipAddress == null || !ipAddress.vmId.equals(vmId) || ipAddress.validUntil.isBefore(Instant.now())) {
            throw new NotFoundException();
        }
        return ipAddress;
    }

    @GET
    @Path("/{vmId}/ipAddresses/{addressId}")
    public IpAddress getIpAddress(@PathParam("vmId") UUID vmId, @PathParam("addressId") long addressId) {
        // getVm does authorization verification
        vmResource.getVm(vmId);
        return getIpAddressInternal(vmId, addressId);
    }


    @GET
    @Path("/{vmId}/ipAddresses")
    public List<IpAddress> getIpAddresses(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        List<IpAddress> ipAddresses = networkService.getVmIpAddresses(virtualMachine.vmId);
        if (ipAddresses != null) {
            ipAddresses = ipAddresses.stream().filter(ip -> ip.validUntil.isAfter(Instant.now())).collect(Collectors.toList());
        }
        return ipAddresses;
    }

    @POST
    @Path("/{vmId}/ipAddresses")
    public VmAction addIpAddress(@PathParam("vmId") UUID vmId,
                                 @ApiParam(value = "internet protocol version - default is 4") @DefaultValue("4") @QueryParam("internetProtocolVersion") int internetProtocolVersion) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);
        validateNoConflictingActions(vmId, actionService, ActionType.ADD_IP);

        validateIPVAddressLimit(internetProtocolVersion, virtualMachine.vmId, virtualMachine.spec);

        logger.info("Adding IP to VM {}", virtualMachine.vmId);
        if (virtualMachine.hfsVmId == 0) {
            throw new NotFoundException("VM was not associated with hfs vm");
        }
        ActionRequest request = generateAddIpOrchestrationRequest(virtualMachine, internetProtocolVersion);

        logger.info("Adding Ip Address with request " + request.toString());

        return createActionAndExecute(actionService, commandService, virtualMachine.vmId, ActionType.ADD_IP,
                request, "Vps4AddIpAddress", user);
    }

    @DELETE
    @Path("/{vmId}/ipAddresses/{addressId}")
    public VmAction destroyIpAddress(@PathParam("vmId") UUID vmId, @PathParam("addressId") long addressId) {
        VirtualMachine virtualMachine = vmResource.getVm(vmId);

        IpAddress ipAddress = getIpAddressInternal(vmId, addressId);

        if (ipAddress.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)) {
            throw new Vps4Exception("CANNOT_DESTROY_PRIMARY_IP", "Cannot destroy a VM's Primary IP");
        }
        ActionRequest request = generateDestroyIpOrchestrationRequest(virtualMachine, addressId);

        return createActionAndExecute(actionService, commandService, virtualMachine.vmId, ActionType.DESTROY_IP,
                request, "Vps4DestroyIpAddressAction", user);
    }

    private ActionRequest generateDestroyIpOrchestrationRequest(VirtualMachine vm, long addressId) {
        Vps4DestroyIpAddressAction.Request request = new Vps4DestroyIpAddressAction.Request();
        request.addressId = addressId;
        request.vmId = vm.vmId;
        return request;
    }

    private ActionRequest generateAddIpOrchestrationRequest(VirtualMachine vm, int internetProtocolVersion) {
        Project project = projectService.getProject(vm.projectId);
        String sgid = project.getVhfsSgid();

        String zone = vm.spec.isVirtualMachine() ?
                config.get(vm.dataCenter.dataCenterName + ".optimizedHosting.zone", null) :
                config.get(vm.dataCenter.dataCenterName + ".ovh.zone", null);

        if (vm.spec.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING
                || (vm.spec.serverType.platform == ServerType.Platform.OVH && internetProtocolVersion == 4)) {
            Vps4AddIpAddress.Request request = new Vps4AddIpAddress.Request();
            request.vmId = vm.vmId;
            request.zone = zone;
            request.sgid = sgid;
            request.serverId = vm.hfsVmId;
            request.internetProtocolVersion = internetProtocolVersion;
            return request;
        } else
            throw new Vps4Exception("ADD_IP_NOT_SUPPORTED_FOR_PLATFORM", String.format("Add IP for IPv %s not supported " +
                    "for platform %s", internetProtocolVersion, vm.spec.serverType.platform));
    }

    private void validateIPVAddressLimit(int ipVersion, UUID vmId, ServerSpec spec) {
        int currentIpsInUse = networkService.getActiveIpAddressesCount(vmId, ipVersion);
        boolean isOH = spec.serverType.platform == ServerType.Platform.OPTIMIZED_HOSTING;
        int ipAddressLimit = getIpAddressLimit(ipVersion, spec, isOH);

        if (currentIpsInUse >= ipAddressLimit) {
            throw new Vps4Exception("IP_LIMIT_REACHED", String.format("This VM's IPv%s limit is %s and it already has %s ips in use.", ipVersion, spec.ipAddressLimit, currentIpsInUse));
        }
    }

    private static int getIpAddressLimit(int ipVersion, ServerSpec spec, boolean isOH) {
        if (ipVersion == 4) {
            return spec.ipAddressLimit;
        } else if (ipVersion == 6) {
            return isOH ? 5 : 1;
        } else {
            throw new Vps4Exception("INVALID_IPV", String.format("%s is not a valid int for IP Version(IPv4 or IPv6 only).", ipVersion));
        }
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/{vmId}/ipAddresses/{addressId}/blacklist")
    public boolean getBlacklistRecord(@PathParam("vmId") UUID vmId, @PathParam("addressId") long addressId) {
        IpAddress ipAddress = getIpAddressInternal(vmId, addressId);
        return ipBlacklistService.isIpBlacklisted(ipAddress.ipAddress);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @DELETE
    @Path("/{vmId}/ipAddresses/{addressId}/blacklist")
    public void deleteBlacklistRecord(@PathParam("vmId") UUID vmId, @PathParam("addressId") long addressId) {
        IpAddress ipAddress = getIpAddressInternal(vmId, addressId);
        ipBlacklistService.removeIpFromBlacklist(ipAddress.ipAddress);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @POST
    @Path("/{vmId}/ipAddresses/{addressId}/blacklist")
    public void createBlacklistRecord(@PathParam("vmId") UUID vmId, @PathParam("addressId") long addressId) {
        IpAddress ipAddress = getIpAddressInternal(vmId, addressId);
        ipBlacklistService.blacklistIp(ipAddress.ipAddress);
    }
}
