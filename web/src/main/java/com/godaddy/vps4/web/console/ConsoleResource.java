package com.godaddy.vps4.web.console;

import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.console.ConsoleService;
import com.godaddy.vps4.console.CouldNotRetrieveConsoleException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.util.Utils;
import com.godaddy.vps4.web.vm.VmResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConsoleResource {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleResource.class);

    private final ConsoleService consoleService;
    private final VmResource vmResource;

    @Inject
    ConsoleResource(ConsoleService consoleService, VmResource vmResource) {
        this.consoleService = consoleService;
        this.vmResource = vmResource;
    }

    public static class Console {
        public String url;

        public Console() {
            this.url = "";
        }

        public Console(String url) {
            this.url = url;
        }
    }

    @GET
    @Path("{vmId}/console")
    @ApiOperation(value = "Get a console url to a Virtual Machine", notes = "Get a console url to a Virtual Machine")
    public Console getConsoleUrl(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId,
            @QueryParam("fromIpAddress") String fromIpAddress,
            @Context HttpHeaders headers,
            @Context HttpServletRequest req) {
        VirtualMachine vm = vmResource.getVm(vmId);

        try {
            String consoleUrl = null;
            if (vm.spec.isVirtualMachine()) {
                consoleUrl = consoleService.getConsoleUrl(vm.hfsVmId);
            } else {
                // DED server's will issue console url only to a particular ip address
                if (StringUtils.isBlank(fromIpAddress)) {
                    fromIpAddress = Utils.getClientIpAddress(headers, req);
                } else {
                    if (!Utils.isIPv4Address(fromIpAddress)) {
                        throw new Vps4Exception("INVALID_CLIENT_IP", "Invalid client IP address.");
                    }
                }

                logger.info("Fetching console url for vm {} using ip {}", vm.vmId, fromIpAddress);
                consoleUrl = consoleService.getConsoleUrl(vm.hfsVmId, fromIpAddress);
            }

            return new Console(consoleUrl);
        } catch (CouldNotRetrieveConsoleException e) {
            logger.error("Failed getting console url for vm {}.  Returning \"\".  Exception: {}", vm.vmId, e);
            return new Console();
        } catch (Exception e) {
            logger.error("Unexpected exception getting console url for vm {}.  Returning \"\".  Exception: {}", vm.vmId,
                    e);
            return new Console();
        }
    }

}