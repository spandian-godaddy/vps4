package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class CreateVm implements Command<CreateVMWithFlavorRequest, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(CreateVm.class);

    final VmService vmService;

    @Inject
    public CreateVm(VmService vmService) {
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
