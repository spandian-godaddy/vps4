package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.cdn.Vps4ModifyCdnSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(name = "Vps4ReviveZombieVm", requestType = Vps4ReviveZombieVm.Request.class, retryStrategy = CommandRetryStrategy.NEVER)
public class Vps4ReviveZombieVm extends ActionCommand<Vps4ReviveZombieVm.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ReviveZombieVm.class);
    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final CdnDataService cdnDataService;

    @Inject
    public Vps4ReviveZombieVm(ActionService actionService, VirtualMachineService virtualMachineService,
                              CreditService creditService, CdnDataService cdnDataService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.cdnDataService = cdnDataService;
    }


    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {

        logger.info("Reviving Zombie VM with ID {}, new credit ID {}", request.vmId, request.newCreditId);

        transferProductMeta(request);

        removeScheduledCleanupJobs(context, request);

        reviveInOurDb(context, request);

        setEcommCommonName(context, request.newCreditId, request.vmId);

        startServer(context, request);

        resumePanoptaMonitoring(context, request);

        getAndUnpauseCdnSites(context, request);

        return null;
    }


    private void setEcommCommonName(CommandContext context, UUID orionGuid, UUID vmId) {
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        context.execute("SetCommonName", ctx -> {
            creditService.setCommonName(orionGuid, virtualMachine.name);
            return null;
        }, Void.class);
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
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(request.vmId);
        context.execute(ResumePanoptaMonitoring.class, virtualMachine);
    }


    public void getAndUnpauseCdnSites(CommandContext context, Request request) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(request.newCreditId);
        List<VmCdnSite> cdnSites = cdnDataService.getActiveCdnSitesOfVm(request.vmId);

        if (cdnSites != null) {
            for (VmCdnSite site : cdnSites) {
                Vps4ModifyCdnSite.Request req = new Vps4ModifyCdnSite.Request();
                req.encryptedCustomerJwt = null;
                req.vmId = request.vmId;
                req.bypassWAF = CdnBypassWAF.DISABLED;
                req.cacheLevel = CdnCacheLevel.CACHING_OPTIMIZED;
                req.shopperId = credit.getShopperId();
                req.siteId = site.siteId;
                context.execute("ModifyCdnSite-" + site.siteId, Vps4ModifyCdnSite.class, req);
            }
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
        context.execute(Vps4DeleteAllScheduledZombieJobsForVm.class, request.vmId);
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
