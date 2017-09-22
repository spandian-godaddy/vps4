package com.godaddy.vps4.orchestration.hfs.vm;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class CreateVmFromSnapshot implements Command<CreateVMWithFlavorRequest, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(CreateVmFromSnapshot.class);

    final VmService vmService;

    @Inject
    public CreateVmFromSnapshot(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, CreateVMWithFlavorRequest request) {

        logger.info("sending HFS VM request: {}", request);

        VmAction vmAction = context.execute("CreateVmHfs", ctx -> vmService.createVmWithFlavor(request));

        context.execute(WaitForVmAction.class, vmAction);

        return vmAction;
    }

}
