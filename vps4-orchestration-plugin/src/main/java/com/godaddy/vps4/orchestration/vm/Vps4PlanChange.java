package com.godaddy.vps4.orchestration.vm;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
        name="Vps4PlanChange",
        requestType=Vps4PlanChange.Request.class)
public class Vps4PlanChange implements Command<Vps4PlanChange.Request, Void>{

    private static final Logger logger = LoggerFactory.getLogger(Vps4PlanChange.class);
    private final VirtualMachineService virtualMachineService;

    @Inject
    public Vps4PlanChange(VirtualMachineService virtualMachineService) {
        this.virtualMachineService = virtualMachineService;
    }

    public static class Request extends VmActionRequest {
        public VirtualMachineCredit credit;
        public VirtualMachine vm;
        public int managedLevel;
    }

    @Override
    public Void execute(CommandContext context, Request req) {
        if(req.vm.managedLevel != req.credit.managedLevel) {
            logger.info("Processing managed level change for account {} to level {}", req.vm.vmId, req.credit.managedLevel);
            updateVirtualMachineManagedLevel(context, req);
        }
        return null;
    }

    private void updateVirtualMachineManagedLevel(CommandContext context, Request req) {
        Map<String, Object> paramsToUpdate = new HashMap<>();
        paramsToUpdate.put("managed_level", req.credit.managedLevel);
        context.execute("UpdateVmManagedLevel", ctx -> {
            virtualMachineService.updateVirtualMachine(req.credit.productId, paramsToUpdate);
            return null;
        }, Void.class);
    }
}
