package com.godaddy.vps4.web.controlPanel;

import java.util.UUID;

import javax.ws.rs.NotFoundException;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;

import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;

public class ControlPanelRequestValidation {

    public static void validateVmExists(VirtualMachine vm, UUID vmId){
        if (vm == null) {
            throw new NotFoundException("VM not found: " + vmId);
        }
    }

    public static void validateCorrectControlPanel(VirtualMachine vm,
            ControlPanel controlPanel, UUID vmId){
        if (vm.image.controlPanel != controlPanel) {
            throw new  Vps4Exception("INVALID_IMAGE", String.format("Image for %s is not %s.", vmId, controlPanel));
        }
    }

    public static VirtualMachine getValidVirtualMachine(Vps4User user,
            PrivilegeService privilegeService,
            VirtualMachineService virtualMachineService,
            VmService hfsVmService, ControlPanel controlPanel, UUID vmId){
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        validateVmExists(vm, vmId);
        validateCorrectControlPanel(vm, controlPanel, vmId);
        Vm hfsVm = hfsVmService.getVm(vm.hfsVmId);
        validateServerIsActive(hfsVm);
        return vm;
    }
}
