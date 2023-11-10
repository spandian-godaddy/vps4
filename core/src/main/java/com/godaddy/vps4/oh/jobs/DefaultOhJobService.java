package com.godaddy.vps4.oh.jobs;

import java.util.Map;
import java.util.UUID;

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.jobs.models.OhJob;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultOhJobService implements OhJobService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultOhJobService.class);
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
        long hfsVmId = virtualMachineService.getHfsVmIdByVmId(vmId);
        return vmService.getVm(hfsVmId);
    }

    @Override
    public OhJob getJob(UUID vmId, UUID jobId) {
        Vm hfsVm = getHfsVm(vmId);
        String zone = hfsVm.resourceRegion;
        return ohApiJobServices.get(zone).getJob(jobId).value();
    }
}
