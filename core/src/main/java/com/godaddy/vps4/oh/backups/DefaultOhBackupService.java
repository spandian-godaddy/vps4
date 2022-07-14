package com.godaddy.vps4.oh.backups;

import java.util.ArrayList;
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

    // The package_uuid is not required by the OH API, but passing it ensures that the backup corresponds with the
    // requested server. This is used to ensure users actually own the backups they access. The other methods in this
    // class don't need to do this because it is assumed that getBackup will be called first.
    @Override
    public OhBackup getBackup(UUID vmId, UUID backupId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        return ohApiBackupServices.get(zone).getBackupWithAuthValidation(packageId, backupId).value();
    }

    @Override
    public List<OhBackup> getBackups(UUID vmId, OhBackupState... states) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        OhApiBackupService ohApiBackupService = ohApiBackupServices.get(zone);
        List<OhBackup> backups = new ArrayList<>();
        for (OhBackupState state : states) {
            try {
                backups.addAll(ohApiBackupService.getBackups(packageId, state).value());
            } catch (NotFoundException ignored) {
                // the OH API returns a 404 when no snapshots exist, even if the VM itself does
            }
        }
        return backups;
    }

    @Override
    public OhBackup createBackup(UUID vmId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        return ohApiBackupServices.get(zone).createBackup(packageId, OhBackupType.FULL).value();
    }

    @Override
    public void deleteBackup(UUID vmId, UUID backupId) {
        Vm hfsVm = getHfsVm(vmId);
        String zone = hfsVm.resourceRegion;
        ohApiBackupServices.get(zone).deleteBackup(backupId);
    }

    @Override
    public OhJob restoreBackup(UUID vmId, UUID backupId) {
        Vm hfsVm = getHfsVm(vmId);
        UUID packageId = UUID.fromString(hfsVm.resourceId);
        String zone = hfsVm.resourceRegion;
        ohApiBackupServices.get(zone).restoreBackup(packageId, backupId, "restore");
        OhBackup backup = ohApiBackupServices.get(zone).getBackup(backupId).value();
        return ohApiJobServices.get(zone).getJob(backup.jobId).value();
    }
}
