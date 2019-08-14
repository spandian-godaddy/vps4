package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.orchestration.scheduler.DeleteScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(name = "Vps4ReviveZombieVm", requestType = Vps4ReviveZombieVm.Request.class, retryStrategy = CommandRetryStrategy.NEVER)
public class Vps4ReviveZombieVm extends ActionCommand<Vps4ReviveZombieVm.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ReviveZombieVm.class);
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;
    private final ScheduledJobService scheduledJobService;
    private final CreditService creditService;
    private final Config config;

    @Inject
    public Vps4ReviveZombieVm(ActionService actionService, VirtualMachineService virtualMachineService,
                              VmService vmService, ScheduledJobService scheduledJobService, CreditService creditService,
                              Config config) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
        this.scheduledJobService = scheduledJobService;
        this.creditService = creditService;
        this.config = config;
    }

    
    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {

        logger.info("Reviving Zombie VM with ID {}, new credit ID {}", request.vmId, request.newCreditId);

        transferProductMeta(request);

        removeScheduledCleanupJobs(context, request);
        
        reviveInOurDb(context, request);

        startServer(context, request);

        resumePanoptaMonitoring(context, request);

        return null;
    }

    private void startServer(CommandContext context, Request request) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(request.vmId);
        if(virtualMachine.spec.isVirtualMachine()) {
            context.execute(StartVm.class, virtualMachine.hfsVmId);
        } else {
            context.execute(EndRescueVm.class, virtualMachine.hfsVmId);
        }
    }

    public void resumePanoptaMonitoring(CommandContext context, Request request) {
        boolean isPanoptaInstallationEnabled = Boolean.valueOf(config.get("panopta.installation.enabled", "false"));
        if (isPanoptaInstallationEnabled) {
            context.execute(ResumePanoptaMonitoring.class, request.vmId);
        }
    }

    private void transferProductMeta(Request request) {
        Map<ProductMetaField, String> productMeta = creditService.getProductMeta(request.oldCreditId);
        creditService.updateProductMeta(request.newCreditId, productMeta);
    }

    private void reviveInOurDb(CommandContext context, Request request) {
        context.execute("ReviveZombieInOurDb", ctx -> {
            virtualMachineService.reviveZombieVm(request.vmId, request.newCreditId);
            return null;
        }, Void.class);
    }

    private void removeScheduledCleanupJobs(CommandContext context, Request request) {
        List<ScheduledJob> jobs = scheduledJobService.getScheduledJobsByType(request.vmId, ScheduledJobType.ZOMBIE);
        
        for(ScheduledJob job : jobs) {
            DeleteScheduledJob.Request req = new DeleteScheduledJob.Request();
            req.jobId = job.id;
            req.jobRequestClass = Vps4ZombieCleanupJobRequest.class;
            context.execute(DeleteScheduledJob.class, req);
        }
    }
    
    public static class Request implements ActionRequest {
        public long actionId;
        public UUID vmId;
        public UUID newCreditId;
        public UUID oldCreditId;

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
