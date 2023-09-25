package com.godaddy.vps4.orchestration.panopta;

import static com.godaddy.vps4.orchestration.panopta.Utils.getTemplateIds;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.HEAD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class ApplyPanoptaTemplates implements Command<ApplyPanoptaTemplates.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(ApplyPanoptaTemplates.class);

    private final Config config;
    private final CreditService creditService;
    private final PanoptaService panoptaService;

    @Inject
    public ApplyPanoptaTemplates(Config config, CreditService creditService, PanoptaService panoptaService) {
        this.config = config;
        this.creditService = creditService;
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext commandContext, ApplyPanoptaTemplates.Request request) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(request.orionGuid);
        String[] templateIds = getTemplateIds(config, credit);
        applyTemplates(request.vmId, request.partnerCustomerKey, request.serverId, templateIds);
        return null;
    }

    private void applyTemplates(UUID vmId, String partnerCustomerKey, long serverId, String[] templateIds) {
        logger.info("applying templates to Panopta server for VM {}.", vmId);

        String[] templates = Arrays.stream(templateIds)
                                   .map(t -> "https://api2.panopta.com/v2/server_template/" + t)
                                   .toArray(String[]::new);
        panoptaService.applyTemplates(serverId, partnerCustomerKey, templates);
    }

    public static class Request {
        public UUID vmId;
        public UUID orionGuid;
        public String partnerCustomerKey;
        public long serverId;
    }
}
