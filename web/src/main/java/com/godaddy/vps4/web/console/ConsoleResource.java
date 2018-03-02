package com.godaddy.vps4.web.console;

import com.godaddy.vps4.console.ConsoleService;
import com.godaddy.vps4.console.CouldNotRetrieveConsoleException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.vm.VmResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConsoleResource {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleResource.class);

    private final ConsoleService consoleService;
    private final VmResource vmResource;

    @Inject
    ConsoleResource(ConsoleService consoleService, VmResource vmResource){
        this.consoleService = consoleService;
        this.vmResource = vmResource;
    }

    public static class Console{
        public String url;
        public Console(){
            this.url = "";
        }
        public Console(String url){
            this.url = url;
        }
    }

    @GET
    @Path("{vmId}/console")
    @ApiOperation(value = "Get a console url to a Virtual Machine", notes = "Get a console url to a Virtual Machine")
    public Console getConsoleUrl(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        try {
            return new Console(consoleService.getConsoleUrl(vm.hfsVmId));
        }catch(CouldNotRetrieveConsoleException e){
            logger.error("Failed getting console url for vm {}.  Returning \"\".  Exception: {}", vm.vmId, e);
            return new Console();
        }catch(Exception e){
            logger.error("Unexpected exception getting console url for vm {}.  Returning \"\".  Exception: {}", vm.vmId, e);
            return new Console();
        }
    }

}