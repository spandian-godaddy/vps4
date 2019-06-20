package com.godaddy.vps4.web.dns;

import static com.godaddy.vps4.web.util.Commands.execute;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.hfs.dns.RdnsRecords.Results;
import com.godaddy.vps4.orchestration.hfs.dns.Vps4ReverseDnsNameRecordRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.BlockServerType;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Consumes(MediaType.APPLICATION_JSON)
@BlockServerType(serverTypes = {ServerType.Type.VIRTUAL})
public class DnsResource {
    private static final Logger logger = LoggerFactory.getLogger(DnsResource.class);
    private final HfsDnsService dnsService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VmResource vmResource;
    private final GDUser user;
    private final ObjectMapperProvider mapperProvider;
    private final ReverseDnsLookup reverseDnsLookup;

    @Inject
    public DnsResource(HfsDnsService dnsService, VmResource vmResource, ActionService actionService,
                       CommandService commandService, GDUser user, ObjectMapperProvider mapperProvider,
                       ReverseDnsLookup reverseDnsLookup) {
        this.dnsService = dnsService;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.mapperProvider = mapperProvider;
        this.reverseDnsLookup = reverseDnsLookup;
    }

    @GET
    @Path("/{vmId}/rdns/{ipAddress}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get the reverse dns name for dedicated server with specified IP address.",
            notes = "Get the reverse dns name for dedicated server with specified IP address.")
    public String getReverseDnsName(@PathParam("vmId") UUID vmId, @PathParam("ipAddress") String ipAddress) {
        String decodedIpAddress = urlDecodeIpAddress(ipAddress);
        VirtualMachine vm = vmResource.getVm(vmId);
        verifyIpAssociatedWithVm(decodedIpAddress, vm);
        logger.info("Getting reverse dns records for vm id : {} ", vm.vmId);
        return Arrays.stream(dnsService.getReverseDnsName(vm.hfsVmId).results).filter(Objects::nonNull).findFirst()
                     .map(Results::getName).orElse(null);
    }


    public static class ReverseDnsNameRequest {
        public String reverseDnsName;
    }

    @PUT
    @Path("/{vmId}/rdns/{ipAddress}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a reverse dns name record (PTR) for dedicated server with an existing A-record.",
            notes = "Create a reverse dns name record (PTR) for dedicated server with an existing A-record.")
    public VmAction createReverseDnsNameRecord(@PathParam("vmId") UUID vmId, @PathParam("ipAddress") String ipAddress,
                                               ReverseDnsNameRequest request) {

        String decodedIpAddress = urlDecodeIpAddress(ipAddress);
        VirtualMachine vm = vmResource.getVm(vmId);
        verifyIpAssociatedWithVm(decodedIpAddress, vm);
        validateServerIsActive(vmResource.getVmFromVmVertical(vm.hfsVmId));
        validateNoConflictingActions(vmId, actionService, ActionType.CREATE_REVERSE_DNS_NAME_RECORD,
                                     ActionType.RESTORE_VM, ActionType.DESTROY_IP);
        reverseDnsLookup.validateReverseDnsName(request.reverseDnsName, ipAddress);

        Vps4ReverseDnsNameRecordRequest reverseDnsNameRequest = new Vps4ReverseDnsNameRecordRequest();
        reverseDnsNameRequest.virtualMachine = vm;
        reverseDnsNameRequest.reverseDnsName = request.reverseDnsName;
        long actionId = actionService.createAction(vm.vmId, ActionType.CREATE_REVERSE_DNS_NAME_RECORD,
                                                   getRequestAsJsonString(reverseDnsNameRequest), user.getUsername());
        logger.info("Action id to create reverse dns name: {}", actionId);
        reverseDnsNameRequest.setActionId(actionId);
        CommandState command =
                execute(commandService, actionService, "Vps4CreateReverseDnsNameRecord", reverseDnsNameRequest);
        logger.info("running {} with command id {}", command.name, command.commandId);
        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }

    private String urlDecodeIpAddress(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            logger.error("Ip Address is not provided.");
            throw new Vps4Exception("MISSING_IP", "Ip Address is not provided.");
        }
        try {
            return URLDecoder.decode(ipAddress, "UTF-8");
        } catch (UnsupportedEncodingException encEx) {
            logger.error("Ip Address could not be decoded. ", encEx);
            throw new Vps4Exception("IP_ADDRESS_NOT_ENCODED", "IP Address could not be decoded.");
        }
    }

    private void verifyIpAssociatedWithVm(String ipAddress, VirtualMachine vm) {
        if (!StringUtils.equalsIgnoreCase(vm.primaryIpAddress.ipAddress, ipAddress)) {
            logger.warn("Ip address {} provided does not match the primary ip address for the vm {}.", ipAddress,
                        vm.primaryIpAddress.ipAddress);
            throw new Vps4Exception("IP_ADDRESS_NOT_ASSOCIATED_WITH_VM",
                                    "Ip address provided does not match the primary ip address for the vm.");
        }
    }

    private String getRequestAsJsonString(Vps4ReverseDnsNameRecordRequest reverseDnsNameRequest) {
        try {
            return mapperProvider.get().writeValueAsString(reverseDnsNameRequest);
        } catch (JsonProcessingException jsonPex) {
            logger.warn("Could not convert request {} to json. ", reverseDnsNameRequest.toString(), jsonPex);
            return new JSONObject().toJSONString();
        }
    }
}
