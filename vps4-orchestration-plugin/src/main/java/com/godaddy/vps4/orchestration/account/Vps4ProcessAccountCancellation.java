package com.godaddy.vps4.orchestration.account;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.cdn.Vps4ModifyCdnSite;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.scheduler.ScheduleZombieVmCleanup;
import com.godaddy.vps4.orchestration.scheduler.ScheduleCancelAccount;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name="Vps4ProcessAccountCancellation",
        requestType=Vps4ProcessAccountCancellation.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProcessAccountCancellation extends ActionCommand<Vps4ProcessAccountCancellation.Request,Void> {

    private CommandContext context;
    final ActionService vmActionService;
    private final VirtualMachineService virtualMachineService;
    private final Config config;
    private final HfsVmTrackingRecordService hfsVmTrackingRecordService;
    private final CdnDataService cdnDataService;

    @Inject
    public Vps4ProcessAccountCancellation(ActionService vmActionService,
                                          VirtualMachineService virtualMachineService,
                                          Config config,
                                          HfsVmTrackingRecordService hfsVmTrackingRecordService,
                                          CdnDataService cdnDataService) {
        super(vmActionService);
        this.vmActionService = vmActionService;
        this.virtualMachineService = virtualMachineService;
        this.config = config;
        this.hfsVmTrackingRecordService = hfsVmTrackingRecordService;
        this.cdnDataService = cdnDataService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {
        this.context = context;
        try {
            if (hasAccountBeenClaimed(request.virtualMachineCredit)) {
                UUID vmId = request.virtualMachineCredit.getProductId();
                Instant validUntil = calculateValidUntil();
                pausePanoptaMonitoring(vmId, request.virtualMachineCredit);
                getAndPauseCdnSites(vmId, request.virtualMachineCredit);
                markVmAsZombie(vmId);
                UUID jobId = scheduleZombieVmCleanup(vmId, validUntil);
                recordJobId(vmId, jobId);
                stopServer(request, vmId);
            }
        } catch (Exception e) {
            rescheduleCancelAccount(request.virtualMachineCredit);
            throw e;
        }

        return null;
    }

    private void rescheduleCancelAccount(VirtualMachineCredit credit) {
        UUID vmId = credit.getProductId();
        context.execute(ScheduleCancelAccount.class, vmId);
    }

    public void pausePanoptaMonitoring(UUID vmId, VirtualMachineCredit credit) {
        PausePanoptaMonitoring.Request pausePanoptaMonitoringRequest = new PausePanoptaMonitoring.Request();
        pausePanoptaMonitoringRequest.vmId = vmId;
        pausePanoptaMonitoringRequest.shopperId = credit.getShopperId();
        context.execute(PausePanoptaMonitoring.class, pausePanoptaMonitoringRequest);
    }

    private boolean hasAccountBeenClaimed(VirtualMachineCredit virtualMachineCredit) {
        return virtualMachineCredit.getProductId() != null;
    }

    private Instant calculateValidUntil() {
        long waitUntil = context.execute("CalculateValidUntil", ctx -> {
            int waitTime = Integer.parseInt(config.get("vps4.zombie.cleanup.waittime"));
            return Instant.now().plus(waitTime, ChronoUnit.DAYS).toEpochMilli();
        }, long.class);
        return Instant.ofEpochMilli(waitUntil);
    }

    private void stopServer(Request request, UUID vmId) {
        VirtualMachine virtualMachine = context.execute(
                "GetVirtualMachine", ctx -> virtualMachineService.getVirtualMachine(vmId), VirtualMachine.class);

        if(virtualMachine.spec.isVirtualMachine()) {
            context.execute(StopVm.class, virtualMachine.hfsVmId);
        } else {
            context.execute(RescueVm.class, virtualMachine.hfsVmId);
        }
        updateHfsVmTrackingRecord(virtualMachine.hfsVmId, request.actionId);
    }

    public void updateHfsVmTrackingRecord(long hfsVmId, long vps4ActionId){
        context.execute("UpdateHfsVmTrackingRecord", ctx -> {
            hfsVmTrackingRecordService.setCanceled(hfsVmId, vps4ActionId);
            return null;
        }, Void.class);
    }

    private void markVmAsZombie(UUID vmId) {
        context.execute("MarkVmAsZombie", ctx -> {
            virtualMachineService.setVmCanceled(vmId);
            return null;
        }, void.class);
    }

    private UUID scheduleZombieVmCleanup(UUID vmId, Instant validUntil) {
        ScheduleZombieVmCleanup.Request req = new ScheduleZombieVmCleanup.Request();
        req.vmId = vmId;
        req.when = validUntil;
        return context.execute(ScheduleZombieVmCleanup.class, req);
    }

    private void recordJobId(UUID vmId, UUID jobId) {
        Vps4RecordScheduledJobForVm.Request req = new Vps4RecordScheduledJobForVm.Request();
        req.jobId = jobId;
        req.vmId = vmId;
        req.jobType = ScheduledJobType.ZOMBIE;
        context.execute("RecordScheduledJobId", Vps4RecordScheduledJobForVm.class, req);
    }

    public void getAndPauseCdnSites(UUID vmId, VirtualMachineCredit credit) {
        List<VmCdnSite> cdnSites = cdnDataService.getActiveCdnSitesOfVm(vmId);
        if (cdnSites != null) {
            for (VmCdnSite site : cdnSites) {
                Vps4ModifyCdnSite.Request req = new Vps4ModifyCdnSite.Request();
                req.vmId = vmId;
                req.bypassWAF = CdnBypassWAF.ENABLED;
                req.cacheLevel = CdnCacheLevel.CACHING_DISABLED;
                req.siteId = site.siteId;
                req.customerId = credit.getCustomerId();
                context.execute("ModifyCdnSite-" + site.siteId, Vps4ModifyCdnSite.class, req);
            }
        }
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public VirtualMachineCredit virtualMachineCredit;
        public String initiatedBy;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }
}
