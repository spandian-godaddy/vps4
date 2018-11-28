package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.vm.WaitForManageVmAction;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

public class StartVm implements Command<Long, Void> {

    final VmService vmService;

    @Inject
    public StartVm(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public Void execute(CommandContext context, Long vmId) {

        VmAction action = context.execute("StartVmHfs", ctx -> vmService.startVm(vmId), VmAction.class);

        context.execute(WaitForManageVmAction.class, action);

        return null;
    }
}
