package com.godaddy.vps4.web.console;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsDedicated;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
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

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.console.ConsoleService;
import com.godaddy.vps4.console.CouldNotRetrieveConsoleException;
import com.godaddy.vps4.orchestration.console.Vps4RequestConsole;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Utils;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;

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

    private final GDUser user;
    private final ActionService actionService;
    private final CommandService commandService;
    private final ConsoleService consoleService;
    private final VmResource vmResource;
    private final VmService vmService;

    @Inject
    ConsoleResource(GDUser user,
                    ActionService actionService,
                    CommandService commandService,
                    ConsoleService consoleService,
                    VmResource vmResource,
                    VmService vmService) {
        this.user = user;
        this.actionService = actionService;
        this.commandService = commandService;
        this.consoleService = consoleService;
        this.vmResource = vmResource;
        this.vmService = vmService;
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
    @Path("/{vmId}/console")
    @ApiOperation(value = "Get a console url to a Virtual Machine", notes = "Get a console url to a Virtual Machine")
    @Deprecated // this will no longer be needed once the UI switches to the new /consoleUrl endpoints
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

    @POST
    @Path("/{vmId}/consoleUrl")
    @ApiOperation(value = "Request a console url to a server", notes = "Request a console url to a server")
    public VmAction requestConsoleUrl(
            @ApiParam(value = "The ID of the selected server", required = true)
            @PathParam("vmId") UUID vmId,
            @QueryParam("fromIpAddress") String fromIpAddress,
            @Context HttpHeaders headers,
            @Context HttpServletRequest req) {
        logger.info("Requesting console URL for VM ID: {}", vmId);

        VirtualMachine vm = vmResource.getVm(vmId);  // Auth validation
        validateServerIsDedicated(vm);
        validateNoConflictingActions(vmId, actionService, ActionType.REQUEST_CONSOLE);

        if (StringUtils.isBlank(fromIpAddress)) {
            fromIpAddress = Utils.getClientIpAddress(headers, req);
        } else {
            if (!Utils.isIPv4Address(fromIpAddress)) {
                throw new Vps4Exception("INVALID_CLIENT_IP", "Invalid client IP address.");
            }
        }

        logger.info("Requesting console url for vm {} using ip {}", vm.vmId, fromIpAddress);

        Vps4RequestConsole.Request request = new Vps4RequestConsole.Request();
        request.vmId = vm.vmId;
        request.hfsVmId = vm.hfsVmId;
        request.fromIpAddress = fromIpAddress;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.REQUEST_CONSOLE,
                                      request, "Vps4RequestConsole", user);
    }

    @GET
    @Path("/{vmId}/consoleUrl")
    @ApiOperation(value = "Get a console url to a server", notes = "Get a console url to a server")
    public Console getConsoleUrl(
            @ApiParam(value = "The ID of the selected server", required = true)
            @PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);

        try {
            String consoleUrl = vmService.getConsole(vm.hfsVmId).url;
            if (StringUtils.isBlank(consoleUrl)) {
                throw new Vps4Exception("EMPTY_CONSOLE_URL", "The console URL is empty");
            }
            return new Console(consoleUrl);
        } catch (InternalServerErrorException | ClientErrorException e) { // HFS currently throws 500s, but will throw 409s. This catches both.
            logger.error("Error getting console url for vm {}. The URL might not have been requested before being retrieved.", vm.vmId);
            throw new Vps4Exception("CONSOLE_URL_FAILED", "Could not get console URL");
        }
    }
}