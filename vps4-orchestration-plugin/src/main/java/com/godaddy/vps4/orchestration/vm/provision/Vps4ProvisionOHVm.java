package com.godaddy.vps4.orchestration.vm.provision;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyOHVm;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "ProvisionOHVm",
        requestType = ProvisionRequest.class,
        responseType = Vps4ProvisionOHVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProvisionOHVm extends Vps4ProvisionVm {

    @Inject
    public Vps4ProvisionOHVm(
            ActionService actionService,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            VmUserService vmUserService,
            NetworkService networkService,
            CreditService creditService,
            Config config,
            HfsVmTrackingRecordService hfsVmTrackingRecordService,
            VmAlertService vmAlertService) {
        super(actionService, vmService, virtualMachineService, vmUserService, networkService,
              creditService, config, hfsVmTrackingRecordService, vmAlertService);
    }

    /* NO IP management for Optimized Hosting Vms. However, we still track vm-ip mapping in VPS4 Db. */
    @Override
    protected String setupPrimaryIp(Vm hfsVm) {
        super.addIpToDb(hfsVm.address.ip_address);
        return hfsVm.address.ip_address;
    }

    /* Optimized Hosting backups are initiated on the OH side */
    @Override
    protected void setupAutomaticBackupSchedule(UUID vps4VmId, String shopperId) {}

    @Override
    protected void destroyVm(Vps4DestroyVm.Request destroyRequest) {
        context.execute(Vps4DestroyOHVm.class, destroyRequest);
    }
}