package com.godaddy.vps4.oh.jobs;

import java.util.Map;
import java.util.UUID;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

public class DefaultOhJobService implements OhJobService {
    private final Map<String, OhApiJobService> ohApiJobServices;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;

    @Inject
    public DefaultOhJobService(Map<String, OhApiJobService> ohApiJobServices,
                               VirtualMachineService virtualMachineService,
                               VmService vmService) {
        this.ohApiJobServices = ohApiJobServices;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
    }

    private Vm getHfsVm(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        return vmService.getVm(vm.hfsVmId);
    }

    @Override
    public OhJob getJob(UUID vmId, UUID jobId) {
        Vm hfsVm = getHfsVm(vmId);
        String zone = hfsVm.resourceRegion;
        return ohApiJobServices.get(zone).getJob(jobId).value();
    }
}
