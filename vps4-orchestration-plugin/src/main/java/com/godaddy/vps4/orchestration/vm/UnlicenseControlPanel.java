package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.hfs.cpanel.UnlicenseCpanel;
import com.godaddy.vps4.orchestration.hfs.plesk.UnlicensePlesk;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import javax.inject.Inject;

public class UnlicenseControlPanel implements Command<VirtualMachine, Void> {

    private VirtualMachineService virtualMachineService;

    @Inject
    public UnlicenseControlPanel(VirtualMachineService virtualMachineService) {
        this.virtualMachineService = virtualMachineService;
    }

    public static class Request {
        VirtualMachine vm;
    }

    @Override
    public Void execute(CommandContext context, VirtualMachine vm) {
        if (vm.hfsVmId > 0) {
            if (virtualMachineService.virtualMachineHasCpanel(vm.vmId)) {
                unlicenseCpanel(vm, context);
            } else if (virtualMachineService.virtualMachineHasPlesk(vm.vmId)) {
                unlicensePlesk(vm, context);
            }
        }
        return null;
    }

    private void unlicensePlesk(VirtualMachine vm, CommandContext context) {
        context.execute(UnlicensePlesk.class, vm.hfsVmId);
    }

    private void unlicenseCpanel(VirtualMachine vm, CommandContext context) {
        context.execute(UnlicenseCpanel.class, vm.hfsVmId);
    }
}
