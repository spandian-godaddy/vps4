package com.godaddy.vps4.web.controlPanel.cpanel;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.cpanel.CPanelAccount;
import com.godaddy.vps4.cpanel.CPanelSession;
import com.godaddy.vps4.cpanel.CpanelClient.CpanelServiceType;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.controlPanel.ControlPanelRequestValidation;
import com.godaddy.vps4.web.vm.VmResource;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CPanelResource {

    private static final Logger logger = LoggerFactory.getLogger(CPanelResource.class);

    final VmResource vmResource;
    final Vps4CpanelService cpanelService;

    @Inject
    public CPanelResource(VmResource vmResource, Vps4CpanelService cpanelService) {
        this.vmResource = vmResource;
        this.cpanelService = cpanelService;
    }

    @GET
    @Path("{vmId}/cpanel/whmSession")
    public CPanelSession getWHMSession(@PathParam("vmId") UUID vmId) {

        logger.info("get WHM session for vmId {}", vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.createSession(vm.hfsVmId, "root", CpanelServiceType.whostmgrd);
//        } catch (CpanelAccessDeniedException e) {
//            // TODO bubble a more specific error to the client
//            // (UI can show a "Authentication issue" error
//            // instead of just generic "something happened")
//        } catch (CpanelTimeoutException e) {
//        } catch (IOException e) {
        } catch (Exception e) {
            logger.warn("Could not provide WHM cpanel session for vmId {} , Exception: {} ", vmId, e);
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
//        } catch (CpanelAccessDeniedException e) {
//            // TODO bubble a more specific error to the client
//            // (UI can show a "Authentication issue" error
//            // instead of just generic "something happened")
//        } catch (CpanelTimeoutException e) {
//        } catch (IOException e) {
        } catch (Exception e) {
            logger.warn("Could not provide cpanel session for vmId {} , Exception: {} ", vmId, e);
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
//        } catch (CpanelAccessDeniedException e) {
//            // TODO bubble a more specific error to the client
//            // (UI can show a "Authentication issue" error
//            // instead of just generic "something happened")
//        } catch (CpanelTimeoutException e) {
//        } catch (IOException e) {
        } catch (Exception e) {
            logger.warn("Could not provide cpanel accounts for vmId {} , Exception: {} ", vmId, e);
        }
        return null;
    }

    private VirtualMachine resolveVirtualMachine(UUID vmId) {
        return ControlPanelRequestValidation.getValidVirtualMachine(vmResource,
                ControlPanel.CPANEL, vmId);
    }

}
