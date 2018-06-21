package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class StopVm implements Command<Long, VmAction> {

    final VmService vmService;

    @Inject
    public StopVm(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, Long vmId) {

        VmAction action =  vmService.stopVm(vmId);

        context.execute(WaitForVmAction.class, action);

        return action;
    }
}
