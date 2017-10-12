package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class DestroyVm implements Command<Long, Void> {

    final VmService vmService;

    @Inject
    public DestroyVm(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public Void execute(CommandContext context, Long vmId) {

        VmAction hfsAction = context.execute("RequestDestroy", ctx -> vmService.destroyVm(vmId), VmAction.class);

        context.execute(WaitForVmAction.class, hfsAction);

        return null;
    }


}
