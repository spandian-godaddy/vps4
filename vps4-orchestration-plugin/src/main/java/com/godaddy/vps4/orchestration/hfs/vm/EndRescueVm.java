package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class EndRescueVm implements Command<Long, Void> {

    final VmService vmService;

    @Inject
    public EndRescueVm(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public Void execute(CommandContext context, Long vmId) {
        VmAction action = context.execute("EndRescueVmHfs", ctx -> vmService.endRescueVm(vmId), VmAction.class);
        context.execute(WaitForVmAction.class, action);

        return null;
    }
}
