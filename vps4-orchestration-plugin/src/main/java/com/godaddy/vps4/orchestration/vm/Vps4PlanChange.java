package com.godaddy.vps4.orchestration.vm;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.panopta.UpdateManagedPanoptaTemplate;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4PlanChange",
        requestType = Vps4PlanChange.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4PlanChange implements Command<Vps4PlanChange.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4PlanChange.class);
    private final VirtualMachineService virtualMachineService;
    private final PanoptaService panoptaService;
    private final PanoptaDataService panoptaDataService;

    @Inject
    public Vps4PlanChange(VirtualMachineService virtualMachineService, PanoptaService panoptaService, PanoptaDataService panoptaDataService) {
        this.virtualMachineService = virtualMachineService;
        this.panoptaService = panoptaService;
        this.panoptaDataService = panoptaDataService;
    }

    public static class Request extends VmActionRequest {
        public VirtualMachineCredit credit;
        public VirtualMachine vm;
        public int managedLevel;
    }

    @Override
    public Void execute(CommandContext context, Request req) {
        if (req.vm.managedLevel != req.credit.getManagedLevel()) {
            logger.info("Processing managed level change for account {} to level {}", req.vm.vmId,
                    req.credit.getManagedLevel());
            updateVirtualMachineManagedLevel(context, req);
            updatePanoptaTemplate(context, req);
        }
        logger.info("Managed level {} for vm {} in request, matches managed level {} in credit. No action taken.",
                req.vm.managedLevel, req.vm.vmId, req.credit.getManagedLevel());
        return null;
    }

    private void updateVirtualMachineManagedLevel(CommandContext context, Request req) {
        Map<String, Object> paramsToUpdate = new HashMap<>();
        paramsToUpdate.put("managed_level", req.credit.getManagedLevel());
        context.execute("UpdateVmManagedLevel", ctx -> {
            virtualMachineService.updateVirtualMachine(req.credit.getProductId(), paramsToUpdate);
            return null;
        }, Void.class);
    }

    private void updatePanoptaTemplate(CommandContext context, Request req) {
        logger.info("Updating Panopta template for account {} to level {}", req.vm.vmId, req.credit.getManagedLevel());

        if (req.vm.managedLevel != req.credit.getManagedLevel() && req.credit.getManagedLevel() == 2) {
            UpdateManagedPanoptaTemplate.Request request = new UpdateManagedPanoptaTemplate.Request();
            PanoptaServerDetails panoptaServerDetails = panoptaDataService.getPanoptaServerDetails(req.vm.vmId);
            request.serverId = panoptaServerDetails.getServerId();
            request.partnerCustomerKey = panoptaServerDetails.getPartnerCustomerKey();
            request.vmId = req.vm.vmId;
            request.orionGuid = req.credit.getEntitlementId();
            request.partnerCustomerKey = panoptaService.getPartnerCustomerKey(req.credit.getShopperId());

            context.execute(UpdateManagedPanoptaTemplate.class, request);
        }
    }
}
