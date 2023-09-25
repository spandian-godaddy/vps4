package com.godaddy.vps4.orchestration.panopta;

import static com.godaddy.vps4.orchestration.panopta.Utils.getServerTemplateId;
import static com.godaddy.vps4.orchestration.panopta.Utils.getTemplateIds;

import java.time.Instant;
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
        PanoptaServerDetails serverDetails = getOrCreateServer();
        applyTemplates(request.vmId, serverDetails);
        installAgentOrFailGracefully(customerDetails.getCustomerKey(), serverDetails.getServerKey());
        return null;
    }

    private PanoptaCustomerDetails createAndGetCustomerInDb(String customerKey) {
        panoptaDataService.createOrUpdatePanoptaCustomer(request.shopperId, customerKey);
        return panoptaDataService.getPanoptaCustomerDetails(request.shopperId);
    }

    private PanoptaCustomerDetails getOrCreateCustomer() {
        PanoptaCustomerDetails customerInDb = panoptaDataService.getPanoptaCustomerDetails(request.shopperId);
        PanoptaCustomer customerInPanopta = panoptaService.getCustomer(request.shopperId);
        if (customerInPanopta == null) {
            if (customerInDb != null) {
                // if customer is destroyed in Panopta, but not cleaned up in our db
                panoptaDataService.setAllPanoptaServersOfCustomerDestroyed(request.shopperId);
                panoptaDataService.checkAndSetPanoptaCustomerDestroyed(request.shopperId);
            }
            // if customer has not been created
            customerInPanopta = createCustomer();
            customerInDb = createAndGetCustomerInDb(customerInPanopta.customerKey);
        } else {
            if(customerInDb != null && !customerInPanopta.customerKey.equals(customerInDb.getCustomerKey())) {
                // if customer is out of sync in different data centers
                panoptaDataService.setAllPanoptaServersOfCustomerDestroyed(request.shopperId);
                panoptaDataService.checkAndSetPanoptaCustomerDestroyed(request.shopperId);
                customerInDb = createAndGetCustomerInDb(customerInPanopta.customerKey);
            } else if (customerInDb == null) {
                // if customer is created in Panopta but not yet in this data center's db
                customerInDb = createAndGetCustomerInDb(customerInPanopta.customerKey);
            }
        }
        return customerInDb;
    }

    private PanoptaCustomer createCustomer() {
        logger.info("Creating new Panopta customer for shopper {}.", request.shopperId);
        try {
            return panoptaService.createCustomer(request.shopperId);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private PanoptaServerDetails getOrCreateServer() {
        PanoptaServerDetails serverInDb = panoptaDataService.getPanoptaServerDetails(request.vmId);
        PanoptaServer serverInPanopta = panoptaService.getServer(request.vmId);
        if (serverInPanopta == null) {
            if (serverInDb != null) {
                panoptaDataService.setPanoptaServerDestroyed(request.vmId);
            }
            Map<Long, String> attributes = getAttributes();
            String[] tags = attributes.values().toArray(new String[0]);
            PanoptaServer server = createServer(tags);
            String templateId = getServerTemplateId(config, credit);
            panoptaDataService.createPanoptaServer(request.vmId, request.shopperId, templateId, server);
            serverInDb = panoptaDataService.getPanoptaServerDetails(request.vmId);
            panoptaService.setServerAttributes(request.vmId, attributes);
        }
        return serverInDb;
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

    private void applyTemplates(UUID vmId, PanoptaServerDetails serverDetails) {
        ApplyPanoptaTemplates.Request request = new ApplyPanoptaTemplates.Request();
        request.vmId = vmId;
        request.orionGuid = credit.getOrionGuid();
        request.partnerCustomerKey = serverDetails.getPartnerCustomerKey();
        request.serverId = serverDetails.getServerId();
        context.execute(ApplyPanoptaTemplates.class, request);
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
        request.templateIds = String.join(",", getTemplateIds(config, credit));
        request.fqdn = this.request.fqdn;
        context.execute(InstallPanoptaAgent.class, request);
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
