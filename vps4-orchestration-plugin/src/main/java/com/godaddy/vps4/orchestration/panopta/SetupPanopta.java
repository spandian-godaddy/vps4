package com.godaddy.vps4.orchestration.panopta;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanoptaAgent;
import com.godaddy.vps4.orchestration.hfs.sysadmin.UninstallPanoptaAgent;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.reseller.ResellerService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class SetupPanopta implements Command<SetupPanopta.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(SetupPanopta.class);

    private final CreditService creditService;
    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;
    private final ResellerService resellerService;
    private final Config config;

    private CommandContext context;
    private Request request;
    private VirtualMachineCredit credit;

    @Inject
    public SetupPanopta(CreditService creditService, PanoptaDataService panoptaDataService,
                        PanoptaService panoptaService, ResellerService resellerService, Config config) {
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
        this.resellerService = resellerService;
        this.config = config;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        this.context = context;
        this.request = request;
        this.credit = creditService.getVirtualMachineCredit(request.orionGuid);
        PanoptaCustomerDetails customerDetails = getOrCreateCustomer();
        String[] templateIds = getTemplateIds();
        PanoptaServerDetails serverDetails = getOrCreateServer(templateIds[0]);
        applyTemplates(serverDetails.getPartnerCustomerKey(), serverDetails.getServerId(), templateIds);
        installAgentOrFailGracefully(customerDetails.getCustomerKey(), serverDetails.getServerKey());
        return null;
    }

    private PanoptaCustomerDetails getOrCreateCustomer() {
        PanoptaCustomerDetails customerDetails = panoptaDataService.getPanoptaCustomerDetails(request.shopperId);
        if (customerDetails != null) {
            panoptaService.setStatus(request.shopperId, "active");
        }
        else {
            PanoptaCustomer customer = panoptaService.getCustomer(request.shopperId);
            if (customer == null) {
                customer = createCustomer();
            }
            panoptaDataService.createPanoptaCustomer(request.shopperId, customer.customerKey);
            customerDetails = panoptaDataService.getPanoptaCustomerDetails(request.shopperId);
        }
        return customerDetails;
    }

    private PanoptaCustomer createCustomer() {
        logger.info("Creating new Panopta customer for shopper {}.", request.shopperId);
        try {
            return panoptaService.createCustomer(request.shopperId);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private PanoptaServerDetails getOrCreateServer(String templateId) {
        PanoptaServerDetails serverDetails = panoptaDataService.getPanoptaServerDetails(request.vmId);
        if (serverDetails == null) {
            Map<Long, String> attributes = getAttributes();
            String[] tags = attributes.values().toArray(new String[0]);
            PanoptaServer server = createServer(tags);
            panoptaDataService.createPanoptaServer(request.vmId, request.shopperId, templateId, server);
            serverDetails = panoptaDataService.getPanoptaServerDetails(request.vmId);
            panoptaService.setServerAttributes(request.vmId, attributes);
        }
        return serverDetails;
    }

    private PanoptaServer createServer(String[] tags) {
        logger.info("Creating new Panopta server for VM {}.", request.vmId);
        try {
            return panoptaService.createServer(request.shopperId,
                                               request.orionGuid,
                                               request.fqdn,
                                               tags);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyTemplates(String partnerCustomerKey, long serverId, String[] templateIds) {
        logger.info("applying templates to Panopta server for VM {}.", request.vmId);

        String[] templates = Arrays.stream(templateIds)
                .map(t -> "https://api2.panopta.com/v2/server_template/" + t)
                .toArray(String[]::new);
        try {
            panoptaService.applyTemplates(serverId, partnerCustomerKey, templates);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private void installAgentOrFailGracefully(String customerKey, String serverKey) {
        try {
            Instant timeOfInstall = Instant.now();
            installAgent(customerKey, serverKey);
            syncAgent(timeOfInstall);
        } catch (Exception e) {
            logger.error("Error while installing Panopta agent for VM: {}. Error details: {}", this.request.vmId, e);
            try {
                // uninstalling the agent greatly improves the chances that a retry will work
                context.execute(UninstallPanoptaAgent.class, this.request.hfsVmId);
            } catch (Exception ignored) {}
            context.execute(RemovePanoptaMonitoring.class, this.request.vmId);
            throw e;
        }
    }

    private void installAgent(String customerKey, String serverKey) {
        InstallPanoptaAgent.Request request = new InstallPanoptaAgent.Request();
        request.hfsVmId = this.request.hfsVmId;
        request.customerKey = customerKey;
        request.serverKey = serverKey;
        request.serverName = this.request.orionGuid.toString();
        request.templates = String.join(",", getTemplateIds());
        request.fqdn = this.request.fqdn;
        context.execute(InstallPanoptaAgent.class, request);
    }

    private String[] getTemplateIds() {
        String templateType = (credit.isManaged()) ? "managed" : credit.hasMonitoring() ? "addon" : "base";
        String templateOS = credit.getOperatingSystem().toLowerCase();

        String serverTemplate = config.get("panopta.api.templates." + templateType + "." + templateOS);
        String dcAlertTemplate = config.get("panopta.api.templates.webhook");
        return new String[] { serverTemplate, dcAlertTemplate };
    }

    private Map<Long, String> getAttributes() {
        Map<Long, String> tags = new HashMap<>();
        long plidAttributeId = Long.parseLong(config.get("panopta.api.attribute.plid"));
        long brandAttributeId = Long.parseLong(config.get("panopta.api.attribute.brand"));
        long productAttributeId = Long.parseLong(config.get("panopta.api.attribute.product"));
        String plid = credit.getResellerId();
        String reseller = resellerService.getResellerDescription(plid);
        if (reseller == null) {
            tags.put(brandAttributeId, "godaddy");
        } else {
            tags.put(brandAttributeId, reseller.toLowerCase().replace(' ', '-'));
        }
        tags.put(plidAttributeId, plid);
        tags.put(productAttributeId, "vps4");
        return tags;
    }

    private void syncAgent(Instant timeOfInstall) {
        WaitForPanoptaAgentSync.Request request = new WaitForPanoptaAgentSync.Request();
        request.timeOfInstall = timeOfInstall;
        request.vmId = this.request.vmId;
        context.execute(WaitForPanoptaAgentSync.class, request);
    }

    public static class Request {
        public UUID vmId;
        public UUID orionGuid;
        public long hfsVmId;
        public String shopperId;
        public String fqdn;
    }
}
