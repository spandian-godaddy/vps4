package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

public class StopVm implements Command<Long, Void> {

    final VmService vmService;

    @Inject
    public StopVm(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public Void execute(CommandContext context, Long vmId) {

        VmAction action = context.execute("StopVmHfs", ctx -> vmService.stopVm(vmId), VmAction.class);
        context.execute(WaitForVmAction.class, action);

        return null;
    }
}
