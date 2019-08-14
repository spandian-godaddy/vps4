package com.godaddy.vps4.orchestration.account;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.scheduler.ScheduleZombieVmCleanup;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.hfs.vm.VmService;


import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name="Vps4ProcessAccountCancellation",
        requestType=Vps4ProcessAccountCancellation.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProcessAccountCancellation extends ActionCommand<Vps4ProcessAccountCancellation.Request,Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ProcessAccountCancellation.class);
    private CommandContext context;

    final ActionService vmActionService;
    private final VirtualMachineService virtualMachineService;
    private final Config config;
    private final VmService vmService;

    @Inject
    public Vps4ProcessAccountCancellation(ActionService vmActionService,
                                          VirtualMachineService virtualMachineService,
                                          Config config,
                                          VmService vmService) {
        super(vmActionService);
        this.vmActionService = vmActionService;
        this.virtualMachineService = virtualMachineService;
        this.config = config;
        this.vmService = vmService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {
        this.context = context;
        try {
            if (hasAccountBeenClaimed(request.virtualMachineCredit)) {
                UUID vmId = request.virtualMachineCredit.getProductId();
                Instant validUntil = calculateValidUntil();
                pausePanoptaMonitoring(vmId);
                markVmAsZombie(vmId);
                UUID jobId = scheduleZombieVmCleanup(vmId, validUntil);
                recordJobId(vmId, jobId);
                stopServer(request, vmId);
            }
        } catch (Exception e) {
            logger.error(
                "Error while handling account cancellation for account: {}. Error details: {}",
                    request.virtualMachineCredit.getOrionGuid(), e);
            throw new RuntimeException(e);
        }

        return null;
    }

    public void pausePanoptaMonitoring(UUID vmId) {
        boolean isPanoptaInstallationEnabled = Boolean.valueOf(config.get("panopta.installation.enabled", "false"));
        if (isPanoptaInstallationEnabled) {
            context.execute(PausePanoptaMonitoring.class, vmId);
        }
    }

    private boolean hasAccountBeenClaimed(VirtualMachineCredit virtualMachineCredit) {
        return virtualMachineCredit.getProductId() != null;
    }

    private Instant calculateValidUntil() {
        long waitUntil = context.execute("CalculateValidUntil", ctx -> {
            int waitTime = Integer.valueOf(config.get("vps4.zombie.cleanup.waittime"));
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
    }

    private void markVmAsZombie(UUID vmId) {
        context.execute("MarkVmAsZombie", ctx -> {
            virtualMachineService.setVmZombie(vmId);
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
