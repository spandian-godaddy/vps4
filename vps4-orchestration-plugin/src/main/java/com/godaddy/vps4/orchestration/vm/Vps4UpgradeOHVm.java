package com.godaddy.vps4.orchestration.vm;

import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.vm.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.ResizeOHVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4UpgradeOHVm",
        requestType= Vps4UpgradeOHVm.Request.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4UpgradeOHVm extends ActionCommand<Vps4UpgradeOHVm.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4UpgradeOHVm.class);

    private final VirtualMachineService virtualMachineService;
    private CommandContext context;
    private Request request;

    @Inject
    public Vps4UpgradeOHVm(ActionService actionService, VirtualMachineService virtualMachineService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.request = request;

        UUID importedUUID = virtualMachineService.getImportedVm(request.virtualMachine.vmId);
        String specName = virtualMachineService.getSpec(request.newTier, ServerType.Platform.OPTIMIZED_HOSTING.getplatformId()).specName;

        boolean isImported = importedUUID != null;
        if (isImported) {
            VirtualMachine vm = virtualMachineService.getVirtualMachine(importedUUID);
            if (vm.image.operatingSystem != Image.OperatingSystem.WINDOWS) specName = specName + ".ct";
        }

        ResizeOHVm.Request resizeOHVmRequest= new ResizeOHVm.Request(request.virtualMachine.hfsVmId, specName);
        context.execute(ResizeOHVm.class, resizeOHVmRequest);

        updateVmDetails();
        logger.info("Upgrade action complete for vm {}", request.virtualMachine.vmId);
        return null;
    }


    private void updateVmDetails() {
        updateVmTierInDb();
    }

    private void updateVmTierInDb() {
        logger.info("Updating tier to match new upgraded VM {}", request.virtualMachine.vmId);

        int newSpecId = virtualMachineService.getSpec(request.newTier, ServerType.Platform.OPTIMIZED_HOSTING.getplatformId()).specId;
        context.execute("UpdateVmTier", ctx -> {
            virtualMachineService.updateVirtualMachine(request.virtualMachine.vmId, Collections.singletonMap("spec_id", newSpecId));
            return null;
        }, Void.class);
    }

    public static class Request extends VmActionRequest{
        public int newTier;

        public Request(){}

        public Request(VirtualMachine vm, int newTier){
            this.virtualMachine = vm;
            this.newTier = newTier;
        }
    }
}
