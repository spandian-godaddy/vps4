package com.godaddy.vps4.web.plesk;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
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

import com.godaddy.vps4.plesk.PleskSession;
import com.godaddy.vps4.plesk.PleskSubscription;
import com.godaddy.vps4.plesk.Vps4PleskService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PleskResource {

    private static final Logger logger = LoggerFactory.getLogger(PleskResource.class);
    
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    final Vps4PleskService pleskService;
    final PrivilegeService privilegeService;
    final VirtualMachineService virtualMachineService;
    final Vps4User user;

    @Inject
    public PleskResource(Vps4PleskService pleskService, PrivilegeService privilegeService, VirtualMachineService virtualMachineService, Vps4User user) {
        this.pleskService = pleskService;
        this.privilegeService = privilegeService;
        this.virtualMachineService = virtualMachineService;
        this.user = user;
    }
    
    @GET
    @Path("{vmId}/plesk/pleskSessionUrl")
    public PleskSession getPleskSessionUrl(@PathParam("vmId") UUID vmId, @QueryParam("fromIpAddress") String fromIpAddress, @Context HttpHeaders headers, @Context HttpServletRequest req) {
        
        logger.info("Getting Plesk session url for vmId {} ", vmId);

        /*
         * If the fromIpAddress is provided as a query parameter, use it. Otherwise:
         * Use the x-forwarded-for http header value to determine public IP of customers.
         * This header is populated by the nginx server for VPS4.
         * If the http header is not available, attempt to use the remote_addr.
         * This code does not work for SSO login of internal users, 
         * since the x-forwarded-for http header does not contain the public ip for internal GD users.
         */
        if(StringUtils.isBlank(fromIpAddress)) {
             List<String> xForwardedFor = headers.getRequestHeader(X_FORWARDED_FOR);
             if(xForwardedFor != null && !xForwardedFor.isEmpty()) {
                 fromIpAddress = xForwardedFor.get(0).split(",")[0];
             } else if(StringUtils.isNotBlank(req.getRemoteAddr())){
                 fromIpAddress = req.getRemoteAddr();
             } else {
                 throw new NotAcceptableException("Unable to determine client IP address from request header. ");
             }
        }

        
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
             return pleskService.getPleskSsoUrl(vm.hfsVmId, fromIpAddress);
        } catch (Exception ex) {
            logger.warn("Could not provide plesk session url for vmId {} , Exception: {} ", vmId, ex);            
        }
        return null;
    }

    @GET
    @Path("{vmId}/plesk/accounts")
    public List<PleskSubscription> listPleskAccounts(@PathParam("vmId") UUID vmId) {
        
        logger.info("Get Plesk site accounts for vmId {} ", vmId);
        
        VirtualMachine vm = resolveVirtualMachine(vmId);
        try {
            logger.info("HFS VM ID: {} ", vm.hfsVmId);
            return pleskService.listPleskAccounts(vm.hfsVmId);
        } catch (Exception ex) {
            logger.warn("Could not provide plesk site accounts for vmId {} , Exception: {}", vmId, ex);            
        }
        return null;
    }

    private VirtualMachine resolveVirtualMachine(UUID vmId) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        if (vm == null) {
            throw new NotFoundException("VM not found: " + vmId);
        }
        return vm;
    }

}
