package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.cdn.Vps4ModifyCdnSite;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

@CommandMetadata(
        name = "Vps4ProcessReinstateServer",
        requestType = VmActionRequest.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProcessReinstateServer extends ActionCommand<VmActionRequest, Void> {

    final ActionService actionService;
    final CreditService creditService;
    final CdnDataService cdnDataService;
    private final Logger logger = LoggerFactory.getLogger(Vps4ProcessReinstateServer.class);

    @Inject
    public Vps4ProcessReinstateServer(ActionService actionService, CreditService creditService, CdnDataService cdnDataService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
        this.cdnDataService = cdnDataService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {
        logger.info("Processing Reinstate Service with Request: {}", request);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(request.virtualMachine.orionGuid);
        if(credit.isAccountSuspended()) {
            throw new RuntimeException(String.format("Account %s cannot be reinstated, it is still suspended",
                    request.virtualMachine.orionGuid));
        }

        creditService.updateProductMeta(request.virtualMachine.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED, null);
        resumePanoptaMonitoring(context, request);
        getAndUnpauseCdnSites(context, request, credit.getCustomerId());
        if(request.virtualMachine.spec.isVirtualMachine())
            reinstateVm(context, request);
        else
            reinstateDed(context, request);
        return null;
    }

    public void resumePanoptaMonitoring(CommandContext context,  VmActionRequest request) {
        context.execute(ResumePanoptaMonitoring.class, request.virtualMachine);
    }

    public void getAndUnpauseCdnSites(CommandContext context, VmActionRequest request, UUID customerId) {
        List<VmCdnSite> cdnSites = cdnDataService.getActiveCdnSitesOfVm(request.virtualMachine.vmId);
        if (cdnSites != null) {
            for (VmCdnSite site : cdnSites) {
                Vps4ModifyCdnSite.Request req = new Vps4ModifyCdnSite.Request();
                req.vmId = request.virtualMachine.vmId;
                req.bypassWAF = CdnBypassWAF.DISABLED;
                req.cacheLevel = CdnCacheLevel.CACHING_OPTIMIZED;
                req.customerId = customerId;
                req.siteId = site.siteId;
                context.execute("ModifyCdnSite-" + site.siteId, Vps4ModifyCdnSite.class, req);
            }
        }
    }

    private void reinstateDed(CommandContext context, VmActionRequest request) {
        context.execute(EndRescueVm.class, request.virtualMachine.hfsVmId);
    }

    protected void reinstateVm(CommandContext context, VmActionRequest request) {
        context.execute(StartVm.class, request.virtualMachine.hfsVmId);
    }
}
