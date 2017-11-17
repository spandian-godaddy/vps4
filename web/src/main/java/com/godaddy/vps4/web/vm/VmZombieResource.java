package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmZombieResource {
    
    private static final Logger logger = LoggerFactory.getLogger(VmZombieResource.class);

    private final GDUser user;
    private final Vps4UserService vps4UserService;
    private final VirtualMachineService virtualMachineService;
    private final VmResource vmResource;
    private final CreditService creditService;
    
    @Inject
    public VmZombieResource(GDUser user,
            VirtualMachineService virtualMachineService,
            Vps4UserService vps4UserService,
            VmResource vmResource,
            CreditService creditService) {
        this.user = user;
        this.vps4UserService = vps4UserService;
        this.virtualMachineService = virtualMachineService;
        this.vmResource = vmResource;
        this.creditService = creditService;
    }
    
    @AdminOnly
    @POST
    @Path("/{vmId}/revive")
    @ApiOperation(value = "Revive a zombie vm whose account has been cancelled but the server has not yet been deleted", 
        notes = "Revive a zombie vm whose account has been cancelled but the server has not yet been deleted")
    public VirtualMachine reviveZombieVm(
            @ApiParam(value = "The ID of the server to revive", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "The ID of the new credit to which the VM will be linked") @QueryParam("newCreditId") UUID newCreditId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        
        VirtualMachineCredit oldCredit = creditService.getVirtualMachineCredit(vm.orionGuid);
        validateOldAccountIsRemoved(vmId, oldCredit);
        
        VirtualMachineCredit newCredit = getAndValidateUserAccountCredit(creditService,
                newCreditId, user.getShopperId());
        validateCreditIsNotInUse(newCredit);
        
        verifyCreditsMatch(oldCredit, newCredit);
        
        updateVirtualMachineRecord(vmId, newCreditId);
        
        return virtualMachineService.getVirtualMachine(vmId);
    }

    private void updateVirtualMachineRecord(UUID vmId, UUID newCreditId) {
        Map<String, Object> paramsToUpdate = new HashMap<>();
        paramsToUpdate.put("orion_guid", newCreditId);
        virtualMachineService.updateVirtualMachine(vmId, paramsToUpdate);
        virtualMachineService.setValidUntilInfinity(vmId);
    }

    private void verifyCreditsMatch(VirtualMachineCredit oldCredit, VirtualMachineCredit newCredit) {
        if(!oldCredit.controlPanel.equalsIgnoreCase(newCredit.controlPanel)) {
            throw new Vps4Exception("CONTROL_PANEL_MISMATCH", "Control panel of old and new credits do not match");
        }
        if(oldCredit.managedLevel != newCredit.managedLevel) {
            throw new Vps4Exception("MANAGED_LEVEL_MISMATCH", "Managed level of the old and new credits do not match");
        }
        if(oldCredit.monitoring != newCredit.monitoring) {
            throw new Vps4Exception("MONITORING_MISMATCH", "Monitoring of the old and new credits do not match");
        }
        if(!oldCredit.operatingSystem.equalsIgnoreCase(newCredit.operatingSystem)) {
            throw new Vps4Exception("OPERATING_SYSTEM_MISMATCH", "Operating system of the old and new credits do not match");
        }
        if(oldCredit.tier != newCredit.tier) {
            throw new Vps4Exception("TIER_MISMATCH", "Tier of the old and new credits do not match");
        }
    }

    private void validateOldAccountIsRemoved(UUID vmId, VirtualMachineCredit oldCredit) {
        if(!oldCredit.isAccountRemoved()) {
            throw new Vps4Exception("OLD_ACCOUNT_NOT_REMOVED", String.format("Cannot revive %s, old account %s is not removed.", vmId, oldCredit.orionGuid));
        }
    }
    
}
