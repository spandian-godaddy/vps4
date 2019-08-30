package com.godaddy.vps4.panopta;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.hazelcast.util.CollectionUtil;

public class DefaultPanoptaService implements PanoptaService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPanoptaService.class);
    private final PanoptaApiCustomerService panoptaApiCustomerService;
    private final PanoptaApiServerService panoptaApiServerService;
    private final PanoptaDataService panoptaDataService;
    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final Config config;

    @Inject
    public DefaultPanoptaService(PanoptaApiCustomerService panoptaApiCustomerService,
                                 PanoptaApiServerService panoptaApiServerService,
                                 PanoptaDataService panoptaDataService,
                                 VirtualMachineService virtualMachineService,
                                 CreditService creditService,
                                 Config config) {
        this.panoptaApiCustomerService = panoptaApiCustomerService;
        this.panoptaApiServerService = panoptaApiServerService;
        this.panoptaDataService = panoptaDataService;
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.config = config;
    }

    @Override
    public PanoptaCustomer createCustomer(UUID vmId)
            throws PanoptaServiceException {

        // prepare a request to create panopta customer
        PanoptaCustomerRequest panoptaCustomerRequest =
                new PanoptaCustomerRequest(virtualMachineService, creditService, config);
        panoptaCustomerRequest.createPanoptaCustomerRequest(vmId);


        // setup the customer request for panopta
        PanoptaApiCustomerRequest panoptaApiCustomerRequest = new PanoptaApiCustomerRequest();
        panoptaApiCustomerRequest.panoptaPackage = panoptaCustomerRequest.getPanoptaPackage();
        panoptaApiCustomerRequest.name = panoptaCustomerRequest.getShopperId();
        panoptaApiCustomerRequest.emailAddress = panoptaCustomerRequest.getEmailAddress();
        panoptaApiCustomerRequest.partnerCustomerKey = panoptaCustomerRequest.getPartnerCustomerKey();

        // perform a POST to create the customer
        panoptaApiCustomerService.createCustomer(panoptaApiCustomerRequest);

        return mapResponseToCustomer(
                getCustomerDetails(panoptaCustomerRequest.getPartnerCustomerKey()));

    }

    private PanoptaCustomer mapResponseToCustomer(PanoptaApiCustomerList.Customer customer) {
        return new PanoptaCustomer(customer.customerKey, customer.partnerCustomerKey);
    }

    private PanoptaApiCustomerList.Customer getCustomerDetails(String partnerCustomerKey)
            throws PanoptaServiceException {

        PanoptaApiCustomerList panoptaApiCustomerList = panoptaApiCustomerService.getCustomer(partnerCustomerKey);
        if (panoptaApiCustomerList != null) {
            return panoptaApiCustomerList.getCustomerList().stream().filter(customer -> StringUtils
                    .equalsIgnoreCase(customer.partnerCustomerKey, partnerCustomerKey)).findFirst()
                                         .orElseThrow(() -> new PanoptaServiceException("NO_MATCHING_CUSTOMER_FOUND",
                                                                                        "No Matching customer found."));
        }
        throw new PanoptaServiceException("NO_MATCHING_CUSTOMER_FOUND", "No Matching customer found.");
    }

    @Override
    public void deleteCustomer(String partnerCustomerKey) throws PanoptaServiceException {
        PanoptaCustomer panoptaCustomer =
                mapResponseToCustomer(getCustomerDetails(partnerCustomerKey));
        panoptaApiCustomerService.deleteCustomer(panoptaCustomer.customerKey);
    }

    public PanoptaServerMetric getServerMetricsFromPanopta(int agentResourceId, int serverId, String timescale,
                                                           String partnerCustomerKey)
            throws PanoptaServiceException {
        try {

            PanoptaServerMetric serverMetricResponse = panoptaApiServerService
                    .getMetricData(serverId, agentResourceId,
                                   timescale, partnerCustomerKey);
            return serverMetricResponse;
        } catch (Exception ex) {
            String errorMessage = "Failed to get server metrics in Panopta.";
            logger.error(errorMessage);
            throw new PanoptaServiceException("GET_SERVER_METRICS_FAILED", errorMessage);
        }
    }

    @Override
    public Map<String, Integer> getAgentResourceIdList(int serverId, String partnerCustomerKey)
            throws PanoptaServiceException {
        try {
            PanoptaAgentResourceList serverAgentResourceList =
                    panoptaApiServerService.getAgentResourceList(serverId, partnerCustomerKey);
            return serverAgentResourceList.returnAgentResourceIdList();
        } catch (Exception ex) {
            String errorMessage = "Failed to get server's agent resource list in Panopta.";
            logger.error(errorMessage);
            throw new PanoptaServiceException("GET_AGENT_RESOURCE_FAILED", errorMessage);
        }
    }

    @Override
    public PanoptaServer getServer(String partnerCustomerKey) throws PanoptaServiceException {
        PanoptaServers panoptaServers = panoptaApiServerService.getPanoptaServers(partnerCustomerKey);
        if (panoptaServers == null || CollectionUtil.isEmpty(panoptaServers.getServers())) {
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in Panopta for partner customer key: " + partnerCustomerKey);
        }
        if (panoptaServers.getServers().size() > 1) {
            throw new PanoptaServiceException("MULTIPLE_PANOPTA_SERVERS_FOUND",
                                              "Multiple servers found for partner customer key: " + partnerCustomerKey);
        }
        return mapServer(partnerCustomerKey, panoptaServers.getServers().stream().findFirst().get());
    }

    private PanoptaServer mapServer(String partnerCustomerKey, PanoptaServers.Server server) {
        long serverId = Integer.parseInt(server.url.substring(server.url.lastIndexOf("/") + 1));
        return new PanoptaServer(partnerCustomerKey, serverId, server.serverKey, server.name, server.fqdn, server.serverGroup, PanoptaServer.Status.valueOf(server.status.toUpperCase()));
    }

    @Override
    public void pauseServerMonitoring(UUID vmId) {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) return;
        long serverId = panoptaDetail.getServerId();
        String partnerCustomerKey = panoptaDetail.getPartnerCustomerKey();
        PanoptaServer server = mapServer(partnerCustomerKey, panoptaApiServerService.getServer((int)serverId, partnerCustomerKey));
        if (server.status == PanoptaServer.Status.ACTIVE) {
            PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest = new PanoptaApiUpdateServerRequest();
            panoptaApiUpdateServerRequest.fqdn = server.fqdn;
            panoptaApiUpdateServerRequest.name = server.name;
            panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
            panoptaApiUpdateServerRequest.status = PanoptaServer.Status.SUSPENDED.toString().toLowerCase();
            logger.info("Setting Panopta server to suspended status");
            panoptaApiServerService.setServerStatus((int)serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
        }
        else {
            logger.info("Panopta server is already in suspended status. No need to update status");
        }
    }

    @Override
    public void resumeServerMonitoring(UUID vmId) {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) return;
        long serverId = panoptaDetail.getServerId();
        String partnerCustomerKey = panoptaDetail.getPartnerCustomerKey();
        PanoptaServer server = mapServer(partnerCustomerKey, panoptaApiServerService.getServer((int)serverId, partnerCustomerKey));
        if (server.status == PanoptaServer.Status.SUSPENDED) {
            PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest = new PanoptaApiUpdateServerRequest();
            panoptaApiUpdateServerRequest.fqdn = server.fqdn;
            panoptaApiUpdateServerRequest.name = server.name;
            panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
            panoptaApiUpdateServerRequest.status = PanoptaServer.Status.ACTIVE.toString().toLowerCase();
            logger.info("Setting Panopta server to active status");
            panoptaApiServerService.setServerStatus((int)serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
        }
        else {
            logger.info("Panopta server is already in active status. No need to update status");
        }
    }

    @Override
    public PanoptaAvailability getAvailability(UUID vmId, String startTime, String endTime) throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        return panoptaApiServerService.getAvailability((int) panoptaDetail.getServerId(),
                                                       panoptaDetail.getPartnerCustomerKey(),
                                                       startTime,
                                                       endTime);
    }
}
