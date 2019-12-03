package com.godaddy.vps4.panopta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;

public class DefaultPanoptaService implements PanoptaService {
    private static final int UNLIMITED = 0;

    private static final Logger logger = LoggerFactory.getLogger(DefaultPanoptaService.class);
    private final Cache<String, CachedMonitoringGraphs> cache;
    private final ExecutorService pool;
    private final PanoptaApiCustomerService panoptaApiCustomerService;
    private final PanoptaApiServerService panoptaApiServerService;
    private final PanoptaDataService panoptaDataService;
    private final Config config;
    private PanoptaCustomerRequest panoptaCustomerRequest;

    @Inject
    public DefaultPanoptaService(@PanoptaExecutorService ExecutorService pool,
                                 CacheManager cacheManager,
                                 PanoptaApiCustomerService panoptaApiCustomerService,
                                 PanoptaApiServerService panoptaApiServerService,
                                 PanoptaDataService panoptaDataService,
                                 PanoptaCustomerRequest panoptaCustomerRequest,
                                 Config config) {
        this.cache = cacheManager.getCache(CacheName.PANOPTA_METRIC_GRAPH,
                                           String.class,
                                           CachedMonitoringGraphs.class);
        this.pool = pool;
        this.panoptaApiCustomerService = panoptaApiCustomerService;
        this.panoptaApiServerService = panoptaApiServerService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaCustomerRequest = panoptaCustomerRequest;
        this.config = config;
    }

    @Override
    public PanoptaCustomer createCustomer(String shopperId)
            throws PanoptaServiceException {

        // prepare a request to create panopta customer
        panoptaCustomerRequest = panoptaCustomerRequest.createPanoptaCustomerRequest(shopperId);

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
    public void deleteCustomer(String shopperId) {
        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(shopperId);
        if (panoptaCustomerDetails != null) {
            logger.info("Deleting customer in Panopta. Panopta Details: {}", panoptaCustomerDetails.toString());
            panoptaApiCustomerService.deleteCustomer(panoptaCustomerDetails.getCustomerKey());
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
    public List<PanoptaGraph> getUsageGraphs(UUID vmId, String timescale) throws PanoptaServiceException {
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        String cacheKey = String.format("%s.usage.%s", vmId, timescale);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey).graphs;
        } else {
            List<PanoptaGraphId> usageIds = getUsageIds(vmId);
            List<Callable<PanoptaGraph>> tasks = new ArrayList<>();
            for (PanoptaGraphId usageId : usageIds) {
                Callable<PanoptaGraph> task = () -> {
                    PanoptaGraph graph = panoptaApiServerService.getUsageGraph(detail.getServerId(),
                                                                               usageId.id,
                                                                               timescale,
                                                                               detail.getPartnerCustomerKey());
                    graph.type = usageId.type;
                    return graph;
                };
                tasks.add(task);
            }
            return resolveGraphsAndUpdateCache(tasks, cacheKey);
        }
    }

    @Override
    public List<PanoptaGraph> getNetworkGraphs(UUID vmId, String timescale) throws PanoptaServiceException {
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        String cacheKey = String.format("%s.network.%s", vmId, timescale);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey).graphs;
        } else {
            List<PanoptaGraphId> networkIds = getNetworkIds(vmId);
            List<Callable<PanoptaGraph>> tasks = new ArrayList<>();
            for (PanoptaGraphId networkId : networkIds) {
                Callable<PanoptaGraph> task = () -> {
                    PanoptaGraph graph = panoptaApiServerService.getNetworkGraph(detail.getServerId(),
                                                                                 networkId.id,
                                                                                 timescale,
                                                                                 detail.getPartnerCustomerKey());
                    graph.type = networkId.type;
                    return graph;
                };
                tasks.add(task);
            }
            return resolveGraphsAndUpdateCache(tasks, cacheKey);
        }
    }

    private List<PanoptaGraph> resolveGraphsAndUpdateCache(List<Callable<PanoptaGraph>> tasks, String cacheKey) {
        List<PanoptaGraph> graphs = new ArrayList<>();
        try {
            List<Future<PanoptaGraph>> futures = pool.invokeAll(tasks, 1, TimeUnit.MINUTES);
            for (Future<PanoptaGraph> future : futures) {
                graphs.add(future.get());
            }
            cache.put(cacheKey, new CachedMonitoringGraphs(graphs));
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not fetch graph from Panopta", e);
        }
        return graphs;
    }

    public static class CachedMonitoringGraphs {
        public List<PanoptaGraph> graphs;

        public CachedMonitoringGraphs() {}

        public CachedMonitoringGraphs(List<PanoptaGraph> graphs) {
            this.graphs = graphs;
        }
    }

    @Override
    public PanoptaServer getServer(String shopperId, String serverKey) throws PanoptaServiceException {
        String partnerCustomerKey = config.get("panopta.api.partner.customer.key.prefix") + shopperId;
        PanoptaServers servers = panoptaApiServerService.getPanoptaServers(partnerCustomerKey, serverKey);
        if (servers == null || servers.getServers().size() == 0) {
            String message =
                    String.format("No servers found for partnerCustomerKey %s, serverKey %s", partnerCustomerKey,
                                  serverKey);
            throw new PanoptaServiceException("SERVER_NOT_FOUND", message);
        }
        return mapServer(partnerCustomerKey, servers.getServers().get(0));
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
        if (panoptaDetails != null) {
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
