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
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@CommandMetadata(
        name = "Vps4ProcessSuspendServer",
        requestType = VmActionRequest.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProcessSuspendServer extends ActionCommand<VmActionRequest, Void> {

    final ActionService actionService;
    final CreditService creditService;
    final CdnDataService cdnDataService;
    private final Logger logger = LoggerFactory.getLogger(Vps4ProcessSuspendServer.class);

    @Inject
    public Vps4ProcessSuspendServer(ActionService actionService, CreditService creditService, CdnDataService cdnDataService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
        this.cdnDataService = cdnDataService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {
        logger.info("Processing Suspend Service with Request: {}", request);

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(request.virtualMachine.orionGuid);
        creditService.updateProductMeta(request.virtualMachine.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED, Boolean.toString(true));

        pausePanoptaMonitoring(context, request);
        getAndPauseCdnSites(context, request, credit.getShopperId());
        if(request.virtualMachine.spec.isVirtualMachine())
            suspendVm(context, request);
        else
            suspendDed(context, request);
        return null;
    }

    private void suspendDed(CommandContext context, VmActionRequest request) {
        context.execute(RescueVm.class, request.virtualMachine.hfsVmId);
    }

    public void pausePanoptaMonitoring(CommandContext context, VmActionRequest request) {
        context.execute(PausePanoptaMonitoring.class, request.virtualMachine.vmId);
    }

    public void getAndPauseCdnSites(CommandContext context, VmActionRequest request, String shopperId) {
        List<VmCdnSite> cdnSites = cdnDataService.getActiveCdnSitesOfVm(request.virtualMachine.vmId);
        if (cdnSites != null) {
            for (VmCdnSite site : cdnSites) {
                Vps4ModifyCdnSite.Request req = new Vps4ModifyCdnSite.Request();
                req.encryptedCustomerJwt = null;
                req.vmId = request.virtualMachine.vmId;
                req.bypassWAF = CdnBypassWAF.ENABLED;
                req.cacheLevel = CdnCacheLevel.CACHING_DISABLED;
                req.shopperId = shopperId;
                req.siteId = site.siteId;
                context.execute("ModifyCdnSite-" + site.siteId, Vps4ModifyCdnSite.class, req);
            }
        }
    }

    protected void suspendVm(CommandContext context, VmActionRequest request) {
        context.execute(StopVm.class, request.virtualMachine.hfsVmId);
    }
}
