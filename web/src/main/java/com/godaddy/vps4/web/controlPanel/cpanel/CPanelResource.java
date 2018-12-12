package com.godaddy.vps4.web.controlPanel.cpanel;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import com.godaddy.vps4.cpanel.CpanelInvalidUserException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
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
            return cpanelService.createSession(vm.hfsVmId, "root", vm.primaryIpAddress, CpanelServiceType.whostmgrd);
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
            return cpanelService.createSession(vm.hfsVmId, username, vm.primaryIpAddress, CpanelServiceType.cpaneld);
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
        } catch (Exception e) {
            logger.warn("Could not provide cpanel accounts for vmId {} , Exception: {} ", vmId, e);
        }
        return null;
    }

    @GET
    @Path("/{vmId}/cpanel/{username}/addOnDomains")
    public List<String> listAddOnDomains(@PathParam("vmId") UUID vmId, @PathParam("username") String username) {

        logger.info("GET listAddOnDomains for user {} on VM: {}", username, vmId);

        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listAddOnDomains(vm.hfsVmId, username);
        } catch (CpanelInvalidUserException e) {
            throw new Vps4Exception("INVALID_CPANEL_USER", e.getMessage());
        }
        catch (Exception e) {
            logger.warn("Could not provide cpanel add on domains for user {} on vmId {} , Exception: {} ", username, vmId, e);
        }
        return null;
    }

    public static class PasswordStrengthRequest {
        public String password;
    }

    @POST
    @Path("/{vmId}/cpanel/passwordStrength")
    public Long calculatePasswordStrength(
        @PathParam("vmId") UUID vmId, PasswordStrengthRequest passwordStrengthRequest) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.calculatePasswordStrength(vm.hfsVmId, passwordStrengthRequest.password);
        } catch (Exception e) {
            logger.warn("Could not calculate cpanel password strength for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("PASSWORD_STRENGTH_CALCULATION_FAILED", e.getMessage(), e);
        }
    }

    public static class CreateAccountRequest {
        public String domainName;
        public String username;
        public String password;
        public String plan;
        public String contactEmail;
    }

    @POST
    @Path("/{vmId}/cpanel/createAccount")
    public void createAccount(
            @PathParam("vmId") UUID vmId, CreateAccountRequest createAccountRequest) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            cpanelService.createAccount(
                vm.hfsVmId, createAccountRequest.domainName, createAccountRequest.username,
                createAccountRequest.password, createAccountRequest.plan, createAccountRequest.contactEmail);
        } catch (Exception e) {
            logger.warn("Could not create cpanel account for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("CREATE_CPANEL_ACCOUNT_FAILED", e.getMessage(), e);
        }
    }

    @GET
    @Path("/{vmId}/cpanel/packages")
    public List<String> listPackages(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = resolveVirtualMachine(vmId);

        try {
            return cpanelService.listPackages(vm.hfsVmId);
        } catch (Exception e) {
            logger.warn("Could not list CPanel packages for vmId {} , Exception: {} ", vmId, e);
            throw new Vps4Exception("LIST_PACKAGES_FAILED", e.getMessage(), e);
        }
    }

    private VirtualMachine resolveVirtualMachine(UUID vmId) {
        return ControlPanelRequestValidation.getValidVirtualMachine(vmResource,
                ControlPanel.CPANEL, vmId);
    }

}
