package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.orchestration.scheduler.DeleteScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJob.ScheduledJobType;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
            name="Vps4ReviveZombieVm",
            requestType=Vps4ReviveZombieVm.Request.class,
            responseType=VirtualMachine.class
)
public class Vps4ReviveZombieVm implements Command<Vps4ReviveZombieVm.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ReviveZombieVm.class);
    private final VirtualMachineService virtualMachineService;
    private final ScheduledJobService scheduledJobService;
    private final CreditService creditService;

    @Inject
    public Vps4ReviveZombieVm(VirtualMachineService virtualMachineService,
            ScheduledJobService scheduledJobService, CreditService creditService) {
        this.virtualMachineService = virtualMachineService;
        this.scheduledJobService = scheduledJobService;
        this.creditService = creditService;
    }

    
    @Override
    public Void execute(CommandContext context, Request request) {
        
        logger.info("Reviving Zombie VM with ID {}, new credit ID {}", request.vmId, request.newCreditId);

        transferProductMeta(request);

        removeScheduledCleanupJobs(context, request);
        
        reviveInOurDb(context, request);

        return null;
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
        @SuppressWarnings("unchecked")
        List<ScheduledJob> jobs = context.execute("getZombieCleanupJob", ctx -> {
            return scheduledJobService.getScheduledJobsByType(request.vmId, ScheduledJobType.ZOMBIE);
        }, List.class);
        
        for(ScheduledJob job : jobs) {
            DeleteScheduledJob.Request req = new DeleteScheduledJob.Request();
            req.jobId = job.id;
            req.jobRequestClass = Vps4ZombieCleanupJobRequest.class;
            context.execute(DeleteScheduledJob.class, req);
        }
    }
    
    public static class Request {
        public UUID vmId;
        public UUID newCreditId;
        public UUID oldCreditId;
    }


}
