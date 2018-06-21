package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.vm.WaitForManageVmAction;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class StartVm implements Command<Long, VmAction> {

    final VmService vmService;

    @Inject
    public StartVm(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, Long vmId) {

        VmAction action = vmService.startVm(vmId);

        context.execute(WaitForManageVmAction.class, action);

        return action;
    }
}
