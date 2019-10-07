package com.godaddy.vps4.panopta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.hazelcast.util.CollectionUtil;

public class DefaultPanoptaService implements PanoptaService {
    private static final int UNLIMITED = 0;

    private static final Logger logger = LoggerFactory.getLogger(DefaultPanoptaService.class);
    private final PanoptaApiCustomerService panoptaApiCustomerService;
    private final PanoptaApiServerService panoptaApiServerService;
    private final PanoptaDataService panoptaDataService;
    private final Config config;
    private PanoptaCustomerRequest panoptaCustomerRequest;

    @Inject
    public DefaultPanoptaService(PanoptaApiCustomerService panoptaApiCustomerService,
                                 PanoptaApiServerService panoptaApiServerService,
                                 PanoptaDataService panoptaDataService,
                                 PanoptaCustomerRequest panoptaCustomerRequest,
                                 Config config) {
        this.panoptaApiCustomerService = panoptaApiCustomerService;
        this.panoptaApiServerService = panoptaApiServerService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaCustomerRequest = panoptaCustomerRequest;
        this.config = config;
    }

    @Override
    public PanoptaCustomer createCustomer(UUID vmId)
            throws PanoptaServiceException {

        // prepare a request to create panopta customer
        panoptaCustomerRequest = panoptaCustomerRequest.createPanoptaCustomerRequest(vmId);

        // setup the customer request for panopta
        PanoptaApiCustomerRequest panoptaApiCustomerRequest = new PanoptaApiCustomerRequest();
        panoptaApiCustomerRequest.setPanoptaPackage(panoptaCustomerRequest.getPanoptaPackage());
        panoptaApiCustomerRequest.setName(panoptaCustomerRequest.getShopperId());
        panoptaApiCustomerRequest.setEmailAddress(panoptaCustomerRequest.getEmailAddress());
        panoptaApiCustomerRequest.setPartnerCustomerKey(panoptaCustomerRequest.getPartnerCustomerKey());
        logger.info("Panopta API customer Request: {}", panoptaApiCustomerRequest.toString());

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
    public void deleteCustomer(UUID vmId) {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        if(panoptaDetails != null) {
            logger.info("Deleting customer in Panopta. Panopta Details: {}", panoptaDetails.toString());
            panoptaApiCustomerService.deleteCustomer(panoptaDetails.getCustomerKey());
        }
    }

    @Override
    public List<PanoptaGraphId> getUsageIds(UUID vmId) {
        List<PanoptaGraphId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getUsageList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED)
                    .getList();
        }
        return ids;
    }

    @Override
    public List<PanoptaGraphId> getNetworkIds(UUID vmId) {
        List<PanoptaGraphId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED)
                    .getList();
        }
        return ids;
    }

    @Override
    public PanoptaServerMetric getServerMetricsFromPanopta(int agentResourceId, int serverId, String timescale,
                                                           String partnerCustomerKey)
            throws PanoptaServiceException {
        try {

            return panoptaApiServerService.getMetricData(serverId, agentResourceId, timescale, partnerCustomerKey);
        } catch (Exception ex) {
            String errorMessage = "Failed to get server metrics in Panopta.";
            logger.error(errorMessage);
            throw new PanoptaServiceException("GET_SERVER_METRICS_FAILED", errorMessage);
        }
    }

    @Override
    public PanoptaServer getServer(UUID vmId) throws PanoptaServiceException {
        // first check to see if the panopta details exist in the vps4 database.
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail != null) {
            logger.info("Attempting to get panopta server details for vm id {} using partner customer key {} ", vmId,
                        panoptaDetail.getPartnerCustomerKey());
            return mapServer(panoptaDetail.getPartnerCustomerKey(), panoptaApiServerService
                    .getServer(panoptaDetail.getServerId(), panoptaDetail.getPartnerCustomerKey()));
        }

        // no panopta related details exist in vps4 db, fire a call to panopta to get those details
        String partnerCustomerKey = config.get("panopta.api.partner.customer.key.prefix") + vmId;
        logger.info("Attempting to get panopta server details using partner customer key {} ", partnerCustomerKey);
        PanoptaServers panoptaServers = panoptaApiServerService.getPanoptaServers(partnerCustomerKey);
        if (panoptaServers == null || CollectionUtil.isEmpty(panoptaServers.getServers())) {
            String errorMessage = "No matching server found in Panopta for partner customer key: " + partnerCustomerKey;
            logger.warn(errorMessage);
            throw new PanoptaServiceException("NO_SERVER_FOUND", errorMessage);
        }
        if (panoptaServers.getServers().size() > 1) {
            String errorMessage = "Multiple servers found for partner customer key: " + partnerCustomerKey;
            logger.warn(errorMessage);
            throw new PanoptaServiceException("MULTIPLE_PANOPTA_SERVERS_FOUND", errorMessage);
        }
        return mapServer(partnerCustomerKey, panoptaServers.getServers().stream().findFirst().get());
    }

    private PanoptaServer mapServer(String partnerCustomerKey, PanoptaServers.Server server) {
        long serverId = Integer.parseInt(server.url.substring(server.url.lastIndexOf("/") + 1));
        return new PanoptaServer(partnerCustomerKey, serverId, server.serverKey, server.name, server.fqdn,
                                 server.serverGroup, PanoptaServer.Status.valueOf(server.status.toUpperCase()));
    }

    @Override
    public void pauseServerMonitoring(UUID vmId) {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            return;
        }
        int serverId = panoptaDetail.getServerId();
        String partnerCustomerKey = panoptaDetail.getPartnerCustomerKey();
        PanoptaServer server =
                mapServer(partnerCustomerKey, panoptaApiServerService.getServer(serverId, partnerCustomerKey));
        if (server.status == PanoptaServer.Status.ACTIVE) {
            PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest = new PanoptaApiUpdateServerRequest();
            panoptaApiUpdateServerRequest.fqdn = server.fqdn;
            panoptaApiUpdateServerRequest.name = server.name;
            panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
            panoptaApiUpdateServerRequest.status = PanoptaServer.Status.SUSPENDED.toString().toLowerCase();
            logger.info("Setting Panopta server to suspended status");
            panoptaApiServerService.setServerStatus(serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
        } else {
            logger.info("Panopta server is already in suspended status. No need to update status");
        }
    }

    @Override
    public void resumeServerMonitoring(UUID vmId) {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            return;
        }
        int serverId = panoptaDetail.getServerId();
        String partnerCustomerKey = panoptaDetail.getPartnerCustomerKey();
        PanoptaServer server =
                mapServer(partnerCustomerKey, panoptaApiServerService.getServer(serverId, partnerCustomerKey));
        if (server.status == PanoptaServer.Status.SUSPENDED) {
            PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest = new PanoptaApiUpdateServerRequest();
            panoptaApiUpdateServerRequest.fqdn = server.fqdn;
            panoptaApiUpdateServerRequest.name = server.name;
            panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
            panoptaApiUpdateServerRequest.status = PanoptaServer.Status.ACTIVE.toString().toLowerCase();
            logger.info("Setting Panopta server to active status");
            panoptaApiServerService.setServerStatus(serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
        } else {
            logger.info("Panopta server is already in active status. No need to update status");
        }
    }

    @Override
    public void removeServerMonitoring(UUID vmId) {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        logger.info("Attempting to delete server from panopta.");
        if(panoptaDetails != null) {
            logger.info("Panopta Details: {}", panoptaDetails.toString());
            panoptaApiServerService.deleteServer(panoptaDetails.getServerId(), panoptaDetails.getPartnerCustomerKey());
        }
    }

    @Override
    public PanoptaAvailability getAvailability(UUID vmId, String startTime, String endTime)
            throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        return panoptaApiServerService.getAvailability(panoptaDetail.getServerId(),
                                                       panoptaDetail.getPartnerCustomerKey(),
                                                       startTime,
                                                       endTime);
    }

    @Override
    public PanoptaOutage getOutage(UUID vmId, String startTime, String endTime, int limit, int offset)
            throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        return panoptaApiServerService.getOutage(panoptaDetail.getServerId(),
                                                 panoptaDetail.getPartnerCustomerKey(),
                                                 startTime,
                                                 endTime,
                                                 limit,
                                                 offset);
    }
}
