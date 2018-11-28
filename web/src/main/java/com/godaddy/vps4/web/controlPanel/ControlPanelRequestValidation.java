package com.godaddy.vps4.web.controlPanel;

import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;

import java.util.UUID;

import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;

import com.godaddy.hfs.vm.Vm;

public interface ControlPanelRequestValidation {


    public static void validateCorrectControlPanel(VirtualMachine vm,
            ControlPanel controlPanel, UUID vmId){
        if (vm.image.controlPanel != controlPanel) {
            throw new Vps4Exception("INVALID_IMAGE", String.format("Image for %s is not %s.", vmId, controlPanel));
        }
    }

    public static VirtualMachine getValidVirtualMachine(VmResource vmResource,
            ControlPanel controlPanel, UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        validateCorrectControlPanel(vm, controlPanel, vmId);

        Vm hfsVm = vmResource.getVmFromVmVertical(vm.hfsVmId);
        validateServerIsActive(hfsVm);
        return vm;
    }
}
