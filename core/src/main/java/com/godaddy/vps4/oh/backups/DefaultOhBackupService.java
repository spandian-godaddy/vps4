package com.godaddy.vps4.oh.backups;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.NotFoundException;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.oh.backups.models.OhBackupType;
import com.godaddy.vps4.oh.jobs.OhApiJobService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

public class DefaultOhBackupService implements OhBackupService {
    private final Map<String, OhApiBackupService> ohApiBackupServices;
    private final Map<String, OhApiJobService> ohApiJobServices;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;

    @Inject
    public DefaultOhBackupService(Map<String, OhApiBackupService> ohApiBackupServices,
                                  Map<String, OhApiJobService> ohApiJobServices,
                                  VirtualMachineService virtualMachineService,
                                  VmService vmService) {
        this.ohApiBackupServices = ohApiBackupServices;
        this.ohApiJobServices = ohApiJobServices;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
    }

    private Vm getHfsVm(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        return vmService.getVm(vm.hfsVmId);
    }

    public List<OhBackup> getBackups(UUID vmId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        OhApiBackupService ohApiBackupService = ohApiBackupServices.get(zone);
        try {
            return ohApiBackupService.getBackups(packageId, OhBackupState.COMPLETE).value();
        } catch (NotFoundException e) {
            // the OH API returns a 404 when no snapshots exist, even if the VM itself does
            return Collections.emptyList();
        }
    }

    public OhBackup getBackup(UUID vmId, UUID backupId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        return ohApiBackupServices.get(zone).getBackup(packageId, backupId).value();
    }

    public OhBackup createBackup(UUID vmId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        return ohApiBackupServices.get(zone).createBackup(packageId, OhBackupType.FULL).value();
    }

    public void deleteBackup(UUID vmId, UUID backupId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        ohApiBackupServices.get(zone).deleteBackup(packageId, backupId);
    }

    public OhJob restoreBackup(UUID vmId, UUID backupId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        ohApiBackupServices.get(zone).restoreBackup(packageId, backupId, "restore");
        OhBackup backup = ohApiBackupServices.get(zone).getBackup(packageId, backupId).value();
        return ohApiJobServices.get(zone).getJob(backup.jobId).value();
    }
}
