package com.godaddy.vps4.orchestration.vm.provision;

import static com.godaddy.vps4.vm.CreateVmStep.GeneratingHostname;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.intent.IntentService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.dns.CreateDnsPtrRecord;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyDedicated;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.Vps4RebootDedicated;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "ProvisionDedicated",
        requestType = ProvisionRequest.class,
        responseType = Vps4ProvisionDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProvisionDedicated extends Vps4ProvisionVm {

    @Inject
    public Vps4ProvisionDedicated(
            ActionService actionService,
            VmService vmService,
            VirtualMachineService virtualMachineService,
            VmUserService vmUserService,
            NetworkService networkService,
            CreditService creditService,
            Config config,
            HfsVmTrackingRecordService hfsVmTrackingRecordService,
            VmAlertService vmAlertService,
            IntentService intentService) {
        super(actionService, 
              vmService, 
              virtualMachineService, 
              vmUserService, 
              networkService,
              creditService, 
              config, 
              hfsVmTrackingRecordService, 
              vmAlertService,
              intentService);
    }

    /* Dedicated server hostname is set to OVH resource id */
    @Override
    protected void generateAndSetHostname(long hfsVmId, String ipAddress, String resourceId) {
        setStep(GeneratingHostname);
        hostname = resourceId;
        super.setHostname(hfsVmId);
    }

    /* NO need to set mail relay quota for dedicated servers */
    @Override
    protected void createMailRelay(String ipAddress) {
    }

    /* No need to configure mail relay for dedicated servers */
    @Override
    protected void configureMailRelay(long hfsVmId) {
    }

    /* NO need to set up auto backups for dedicated servers */
    @Override
    protected void setupAutomaticBackupSchedule(UUID vps4VmId, String shopperId) {
    }

    /* NO IP management for Dedicated servers. However, we still track vm-ip mapping in VPS4 Db. */
    @Override
    protected String setupPrimaryIp(Vm hfsVm) {
        super.addIpToDb(hfsVm.address.ip_address);
        return hfsVm.address.ip_address;
    }

    @Override
    protected void createPTRRecord(String resourceId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmInfo.vmId);
        CreateDnsPtrRecord.Request reverseDnsNameRequest = new CreateDnsPtrRecord.Request();
        reverseDnsNameRequest.virtualMachine = vm;
        reverseDnsNameRequest.reverseDnsName = resourceId;
        context.execute("CreateDnsPtrRecord", CreateDnsPtrRecord.class, reverseDnsNameRequest);
    }

    @Override
    protected void rebootServer(VmActionRequest rebootRequest) {
        context.execute(Vps4RebootDedicated.class, rebootRequest);
    }

    @Override
    protected void destroyVm(Vps4DestroyVm.Request destroyRequest) {
        context.execute(Vps4DestroyDedicated.class, destroyRequest);
    }
}
