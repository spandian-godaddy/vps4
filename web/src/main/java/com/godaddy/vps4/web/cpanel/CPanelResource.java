package com.godaddy.vps4.web.cpanel;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.cpanel.CPanelAccount;
import com.godaddy.vps4.cpanel.CPanelSession;
import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CPanelResource {

    private static final Logger logger = LoggerFactory.getLogger(CPanelResource.class);

    final Vps4CpanelService cpanelService;
    final PrivilegeService privilegeService;
    final VirtualMachineService virtualMachineService;
    final Vps4User user;

    @Inject
    public CPanelResource(Vps4CpanelService cpanelService, PrivilegeService privilegeService,
            VirtualMachineService virtualMachineService, Vps4User user) {
        this.cpanelService = cpanelService;
        this.privilegeService = privilegeService;
        this.virtualMachineService = virtualMachineService;
        this.user = user;
    }

    @GET
    @Path("{vmId}/cpanel/whmSession")
    public CPanelSession getWHMSession(@PathParam("vmId") UUID vmId) {

        logger.info("get WHM session for vmId {}", vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);
        
        try {
            return cpanelService.createSession(vm.hfsVmId, "root", CpanelServiceType.whostmgrd);
        } catch (CpanelAccessDeniedException e) {
            // TODO bubble a more specific error to the client
            // (UI can show a "Authentication issue" error
            // instead of just generic "something happened")
        } catch (CpanelTimeoutException e) {

        } catch (IOException e) {

        }
        return null;
    }

    @GET
    @Path("{vmId}/cpanel/cpanelSession")
    public CPanelSession getCPanelSession(@PathParam("vmId") UUID vmId, @QueryParam("username") String username) {

        logger.info("get cPanel session for vmId {}", vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.createSession(vm.hfsVmId, username, CpanelServiceType.cpaneld);
        } catch (CpanelAccessDeniedException e) {
            // TODO bubble a more specific error to the client
            // (UI can show a "Authentication issue" error
            // instead of just generic "something happened")
        } catch (CpanelTimeoutException e) {

        } catch (IOException e) {

        }
        return null;
    }

    @GET
    @Path("/{vmId}/cpanel/accounts")
    public List<CPanelAccount> listCpanelAccounts(@PathParam("vmId") UUID vmId) {

        logger.info("GET listCpanelAccounts for VM: {}", vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listCpanelAccounts(vm.hfsVmId);
        } catch (CpanelAccessDeniedException e) {
            // TODO bubble a more specific error to the client
        } catch (CpanelTimeoutException e) {

        } catch (IOException e) {

        }
        return null;
    }

    VirtualMachine resolveVirtualMachine(UUID vmId) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        if (vm == null) {
            throw new NotFoundException("VM not found: " + vmId);
        }
        if (vm.image.controlPanel != ControlPanel.CPANEL) {
            throw new  Vps4Exception("INVALID_IMAGE", String.format("Image for %s is not cPanel.", vmId));
        }
        return vm;
    }

}
