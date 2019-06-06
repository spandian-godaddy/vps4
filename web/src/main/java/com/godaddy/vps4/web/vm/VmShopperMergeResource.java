package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.project.UserProjectPrivilege;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
public class VmShopperMergeResource {

    private static final Logger logger = LoggerFactory.getLogger(VmShopperMergeResource.class);
    private final VmResource vmResource;
    private final GDUser newUser;
    private final Vps4UserService vps4UserService;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;
    private final PrivilegeService privilegeService;


    @Inject
    public VmShopperMergeResource(
            GDUser newUser,
            Vps4UserService vps4UserService,
            CreditService creditService,
            VirtualMachineService virtualMachineService,
            PrivilegeService privilegeService,
            VmResource vmResource
    ) {

        this.newUser = newUser;
        this.vps4UserService = vps4UserService;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.vmResource = vmResource;
    }

    public static class ShopperMergeRequest {
        public String newShopperId;
    }


    @POST
    @Path("{vmId}/mergeShopper")
    @ApiOperation(value = "merges two shopper accounts.",
            notes = "merges two shopper accounts.")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public void mergeTwoShopperAccounts(@PathParam("vmId") UUID vmId, ShopperMergeRequest shopperMergeRequest) {
        logger.info("attempting to merge shopperId {} with vmId {}", shopperMergeRequest.newShopperId, vmId);
        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit vmCredit = getAndValidateUserAccountCredit(
                creditService, vm.orionGuid, shopperMergeRequest.newShopperId);

        Vps4User vps4NewUser =
                vps4UserService.getOrCreateUserForShopper(shopperMergeRequest.newShopperId, vmCredit.getResellerId());

        long projectId = vm.projectId;

        UserProjectPrivilege currentShopperProjectPrivilege = privilegeService.getActivePrivilege(projectId);
        if (currentShopperProjectPrivilege == null) {
            throw new Vps4Exception("NO_SHOPPER_PRIVILEGE", "shopper privilege not found");
        }
        long currentUserId = currentShopperProjectPrivilege.vps4UserId;
        privilegeService.outdateVmPrivilegeForShopper(currentUserId, projectId);
        privilegeService
                .addPrivilegeForUser(vps4NewUser.getId(), currentShopperProjectPrivilege.privilegeId, projectId);
    }

}
