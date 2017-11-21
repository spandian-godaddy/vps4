package com.godaddy.vps4.orchestration.account;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.scheduler.ScheduleZombieVmCleanup;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4StopVm;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@CommandMetadata(
        name="Vps4ProcessAccountCancellation",
        requestType=VirtualMachineCredit.class
)
public class Vps4ProcessAccountCancellation implements Command<VirtualMachineCredit, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ProcessAccountCancellation.class);
    private CommandContext context;

    final ActionService vmActionService;
    private final VirtualMachineService virtualMachineService;
    private final ScheduledJobService scheduledJobService;
    private final Config config;

    @Inject
    public Vps4ProcessAccountCancellation(ActionService vmActionService,
                                          VirtualMachineService virtualMachineService,
                                          ScheduledJobService scheduledJobService,
                                          Config config) {
        this.vmActionService = vmActionService;
        this.virtualMachineService = virtualMachineService;
        this.scheduledJobService = scheduledJobService;
        this.config = config;
    }

    @Override
    public Void execute(CommandContext context, VirtualMachineCredit virtualMachineCredit) {
        this.context = context;
        try {
            if (hasAccountBeenClaimed(virtualMachineCredit)) {
                UUID vmId = virtualMachineCredit.productId;
                Instant validUntil = calculateValidUntil();
                stopVirtualMachine(vmId);
                markVmAsZombie(vmId, validUntil);
                UUID jobId = scheduleZombieVmCleanup(vmId, validUntil);
                recordJobId(vmId, jobId);
            }
        } catch (Exception e) {
            logger.error(
                "Error while handling account cancellation for account: {}. Error details: {}",
                virtualMachineCredit.orionGuid, e);
            throw new RuntimeException(e);
        }

        return null;
    }

    private boolean hasAccountBeenClaimed(VirtualMachineCredit virtualMachineCredit) {
        return virtualMachineCredit.productId != null;
    }

    private Instant calculateValidUntil() {
        long waitUntil = context.execute("CalculateValidUntil", ctx -> {
            int waitTime = Integer.valueOf(config.get("vps4.zombie.cleanup.waittime"));
            return Instant.now().plus(waitTime, ChronoUnit.DAYS).toEpochMilli();
        }, long.class);
        return Instant.ofEpochMilli(waitUntil);
    }

    private void stopVirtualMachine(UUID vmId) {
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        long actionId = context.execute(
        "CreateVmStopAction",
            ctx -> vmActionService.createAction(vmId, ActionType.STOP_VM, new JSONObject().toJSONString(), vps4UserId),
            long.class);
        long hfsVmId = context.execute(
         "GetHfsVmId", ctx -> virtualMachineService.getVirtualMachine(vmId).hfsVmId, long.class);

        VmActionRequest request = new VmActionRequest();
        request.hfsVmId = hfsVmId;
        request.actionId = actionId;
        context.execute(Vps4StopVm.class, request);
    }

    private void markVmAsZombie(UUID vmId, Instant validUntil) {
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
        context.execute("RecordScheduledJobId", ctx -> {
            scheduledJobService.insertScheduledJob(jobId, vmId, ScheduledJobType.ZOMBIE);
            return null;
        }, void.class);
    }
}
