package com.godaddy.vps4.oh;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.models.OhBackup;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

public class DefaultOhBackupService implements OhBackupService {
    private final Map<String, OhApiBackupService> ohApiBackupServices;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;

    @Inject
    public DefaultOhBackupService(Map<String, OhApiBackupService> ohApiBackupServices,
                                  VirtualMachineService virtualMachineService,
                                  VmService vmService) {
        this.ohApiBackupServices = ohApiBackupServices;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
    }

    private OhApiBackupService getService(String zone) {
        OhApiBackupService service = ohApiBackupServices.get(zone);
        if (service == null) {
            throw new RuntimeException("OH backup service for zone \"" + zone + "\" does not exist");
        }
        return service;
    }

    public List<OhBackup> getBackups(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        Vm hfsVm = vmService.getVm(vm.hfsVmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        try {
            return getService(zone).getBackups(packageId).response.data;
        } catch (NotFoundException ignored) {
            // the OH API returns a 404 when no snapshots exist, even if the VM itself does
            return Collections.emptyList();
        }
    }
}
