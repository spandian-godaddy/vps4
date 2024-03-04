package com.godaddy.vps4.web.vm;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.RequiresRole;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmPatchResource {

    private static final Logger logger = LoggerFactory.getLogger(VmPatchResource.class);

    private final VirtualMachineService virtualMachineService;
    private final ActionService actionService;
    private final CreditService creditService;
    private final ShopperNotesService shopperNotesService;
    private final VmResource vmResource;
    private final GDUser user;

    @Inject
    public VmPatchResource(GDUser user, 
                           VirtualMachineService virtualMachineService,
                           ActionService actionService,
                           CreditService creditService,
                           ShopperNotesService shopperNotesService,
                           VmResource vmResource) {
        this.virtualMachineService = virtualMachineService;
        this.actionService = actionService;
        this.creditService = creditService;
        this.shopperNotesService = shopperNotesService;
        this.vmResource = vmResource;
        this.user = user;
    }

    public static class VmPatch {
        public String name;
    }

    @PATCH
    @Path("/{vmId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Update VM Attributes", httpMethod = "PATCH")
    public VmAction updateVm(@PathParam("vmId") UUID vmId, VmPatch vmPatch) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation

        Map<String, Object> vmPatchMap = new HashMap<>();
        StringBuilder notes = new StringBuilder();
        if (vmPatch.name != null && !vmPatch.name.equals("")){
            vmPatchMap.put("name", vmPatch.name);
            notes.append(vmPatch.name);
        }
        logger.info("Updating vm {}'s with {} ", vmId, vmPatchMap);

        long actionId = this.actionService.createAction(vmId, ActionType.UPDATE_SERVER, new JSONObject().toJSONString(), user.getUsername());
        virtualMachineService.updateVirtualMachine(vmId, vmPatchMap);
        creditService.setCommonName(vm.orionGuid, vmPatch.name);
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), notes.toString());
        return new VmAction(this.actionService.getAction(actionId), user.isEmployee());
    }

    private void writeShopperNote(UUID vmId, UUID creditId) {
        try {
            String shopperNote = String.format("Server was migrated from India to Singapore data center by %s. VM ID: %s. Credit ID: %s.",
                    user.getUsername(), vmId.toString(),
                    creditId.toString());
            shopperNotesService.processShopperMessage(vmId, shopperNote);
        } catch (Exception ignored) {}
    }

    //TODO: This is hardcoded for the GPE migration from bom to sin, remove once migration is over
    @PATCH
    @RequiresRole(roles = { GDUser.Role.ADMIN, GDUser.Role.IMPORT })
    @Path("/{vmId}/dcMigration")
    @Produces({ "application/json" })
    @ApiOperation(value = "Update VM Data Center Info", httpMethod = "PATCH")
    public void updateVmDataCenter(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation

        if (vm.dataCenter.dataCenterId != 5) {
            throw new Vps4Exception("WRONG_ORIGIN_DC", "VM is not in India DC");
        }

        Map<String, Object> vmPatchMap = new HashMap<>();
        vmPatchMap.put("data_center_id", 3);

        logger.info("Updating vm {}'s DC from India to Singapore", vmId);

        virtualMachineService.updateVirtualMachine(vmId, vmPatchMap);
        Map<ECommCreditService.ProductMetaField, String> newProdMeta = new EnumMap<>(ECommCreditService.ProductMetaField.class);
        newProdMeta.put(ECommCreditService.ProductMetaField.DATA_CENTER, String.valueOf(3));
        creditService.updateProductMeta(vm.orionGuid, newProdMeta);
        writeShopperNote(vmId, vm.orionGuid);
    }

}
