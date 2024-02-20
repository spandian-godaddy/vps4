package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

import static com.godaddy.vps4.orchestration.panopta.Utils.getNonManagedTemplateId;
import static com.godaddy.vps4.orchestration.panopta.Utils.getServerTemplateId;

public class UpdateManagedPanoptaTemplate implements Command<UpdateManagedPanoptaTemplate.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(UpdateManagedPanoptaTemplate.class);

    private final Config config;
    private final CreditService creditService;
    private final PanoptaService panoptaService;

    @Inject
    public UpdateManagedPanoptaTemplate(Config config, CreditService creditService, PanoptaService panoptaService) {
        this.config = config;
        this.creditService = creditService;
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext commandContext, Request request) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(request.orionGuid);

        String nonManagedTemplate = getNonManagedTemplateId(config, credit);
        String managedTemplate = getServerTemplateId(config, credit);

        applyTemplate(request.vmId, request.partnerCustomerKey, request.serverId, managedTemplate);
        removeTemplate(request.vmId, request.partnerCustomerKey, request.serverId, nonManagedTemplate);

        return null;
    }

    private void removeTemplate(UUID vmId, String partnerCustomerKey, long serverId, String templateId) {
        logger.info("Removing template from Panopta server for VM {} for serverId {}.", vmId, serverId);

        panoptaService.removeTemplate(serverId, partnerCustomerKey, templateId, "delete");
    }

    private void applyTemplate(UUID vmId, String partnerCustomerKey, long serverId, String templateId) {
        logger.info("Applying templates to Panopta server for VM {} for serverId {}.", vmId, serverId);

        String[] template = {"https://api2.panopta.com/v2/server_template/" + templateId};
        panoptaService.applyTemplates(serverId, partnerCustomerKey, template);
    }

    public static class Request {
        public UUID vmId;
        public UUID orionGuid;
        public String partnerCustomerKey;
        public long serverId;
    }
}
