package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final DateTimeFormatter PANOPTA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final int UNLIMITED = 0;
    private static final int SLEEP_SECONDS = 10;
    private static final int TIMEOUT_MINUTES = 1;

    private static final Logger logger = LoggerFactory.getLogger(DefaultPanoptaService.class);
    private final Cache<String, CachedMonitoringGraphs> cache;
    private final ExecutorService pool;
    private final PanoptaApiCustomerService panoptaApiCustomerService;
    private final PanoptaApiServerService panoptaApiServerService;
    private final PanoptaApiServerGroupService panoptaApiServerGroupService;
    private final PanoptaDataService panoptaDataService;
    private final Config config;

    @Inject
    public DefaultPanoptaService(@PanoptaExecutorService ExecutorService pool,
                                 CacheManager cacheManager,
                                 PanoptaApiCustomerService panoptaApiCustomerService,
                                 PanoptaApiServerService panoptaApiServerService,
                                 PanoptaApiServerGroupService panoptaApiServerGroupService,
                                 PanoptaDataService panoptaDataService,
                                 Config config) {
        this.cache = cacheManager.getCache(CacheName.PANOPTA_METRIC_GRAPH,
                                           String.class,
                                           CachedMonitoringGraphs.class);
        this.pool = pool;
        this.panoptaApiCustomerService = panoptaApiCustomerService;
        this.panoptaApiServerService = panoptaApiServerService;
        this.panoptaApiServerGroupService = panoptaApiServerGroupService;
        this.panoptaDataService = panoptaDataService;
        this.config = config;
    }

    @Override
    public PanoptaCustomer createCustomer(String shopperId) throws PanoptaServiceException {
        String emailAddress = config.get("panopta.api.customer.email");
        String panoptaNamePrefix = config.get("panopta.api.name.prefix");
        String panoptaPackage = config.get("panopta.api.package");
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);

        PanoptaApiCustomerRequest panoptaApiCustomerRequest = new PanoptaApiCustomerRequest();
        panoptaApiCustomerRequest.setPanoptaPackage(panoptaPackage);
        panoptaApiCustomerRequest.setName(panoptaNamePrefix + shopperId);
        panoptaApiCustomerRequest.setEmailAddress(emailAddress);
        panoptaApiCustomerRequest.setPartnerCustomerKey(partnerCustomerKey);
        logger.info("Create Panopta customer Request: {}", panoptaApiCustomerRequest);

        panoptaApiCustomerService.createCustomer(panoptaApiCustomerRequest);
        return mapResponseToCustomer(getCustomerDetails(partnerCustomerKey));
    }

    @Override
    public PanoptaCustomer getCustomer(String shopperId) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        PanoptaApiCustomerList panoptaApiCustomerList =
                panoptaApiCustomerService.getCustomersByStatus(partnerCustomerKey,"active");
        if (!panoptaApiCustomerList.getCustomerList().isEmpty()) {
                return mapResponseToCustomer(panoptaApiCustomerList.getCustomerList().stream().findFirst().get());
        }
        logger.info("Could not find customer in panopta for partner-customer-key {} ", partnerCustomerKey);
        return null;
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
            logger.info("Deleting customer in Panopta. Panopta Details: {}", panoptaCustomerDetails);
            panoptaApiCustomerService.deleteCustomer(panoptaCustomerDetails.getCustomerKey());
        }
    }

    @Override
    public PanoptaServer createServer(String shopperId,
                                      UUID orionGuid,
                                      String ipAddress,
                                      String[] templates,
                                      String[] tags) throws PanoptaServiceException {
        PanoptaApiServerRequest request = new PanoptaApiServerRequest(ipAddress, orionGuid.toString(),
                                                                      getDefaultGroup(shopperId), templates, tags);

        logger.info("Create Panopta server request: {}", request);
        panoptaApiServerService.createServer(getPartnerCustomerKey(shopperId), request);

        Instant timeoutAt = Instant.now().plus(TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        while (Instant.now().isBefore(timeoutAt)) {
            List<PanoptaServer> servers = getServers(shopperId, ipAddress, orionGuid);
            if (!servers.isEmpty()) {
                return servers.get(0);
            }
            try {
                Thread.sleep(SLEEP_SECONDS * 1000);
            } catch (InterruptedException ignored) {}
        }
        throw new PanoptaServiceException("NO_SERVER_FOUND", "No matching server found.");
    }

    @Override
    public PanoptaServer getServer(UUID vmId) {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetails != null) {
            long serverId = panoptaDetails.getServerId();
            String partnerCustomerKey = panoptaDetails.getPartnerCustomerKey();
            return mapServer(partnerCustomerKey, panoptaApiServerService.getServer(serverId, partnerCustomerKey));
        }
        logger.info("Could not find server in panopta for VM ID {} ", vmId);
        return null;
    }

    @Override
    public List<PanoptaServer> getServers(String shopperId, String ipAddress, UUID orionGuid) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        List<PanoptaServer> panoptaServerList = new ArrayList<>();
        PanoptaServers ps = panoptaApiServerService.getServers(partnerCustomerKey,
                                                               ipAddress,
                                                               orionGuid.toString());
        ps.getServers().forEach(server -> panoptaServerList.add(mapServer(partnerCustomerKey, server)));
        return panoptaServerList;
    }

    @Override
    public void deleteServer(UUID vmId) {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetails != null) {
            logger.info("Attempting to delete server {} from panopta.", panoptaDetails.getServerId());
            panoptaApiServerService.deleteServer(panoptaDetails.getServerId(), panoptaDetails.getPartnerCustomerKey());
        }
    }

    @Override
    public void setServerAttributes(UUID vmId, Map<Long, String> attributes) {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        attributes.forEach((key, value) -> {
            PanoptaApiAttributeRequest request = new PanoptaApiAttributeRequest(key, value);
            panoptaApiServerService.setAttribute(panoptaDetails.getServerId(),
                                                 panoptaDetails.getPartnerCustomerKey(),
                                                 request);
        });
    }

    @Override
    public List<PanoptaGraphId> getUsageIds(UUID vmId) {
        List<PanoptaGraphId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getUsageList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED)
                    .value;
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
                    .value;
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
                    graph.metadata = usageId.metadata;
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
                    graph.metadata = networkId.metadata;
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

    private String getPartnerCustomerKey(String shopperId) {
        return config.get("panopta.api.partner.customer.key.prefix") + shopperId;
    }

    private PanoptaServer mapServer(String partnerCustomerKey, PanoptaServers.Server server) {
        long serverId = Long.parseLong(server.url.substring(server.url.lastIndexOf("/") + 1));
        PanoptaServer.Status status = PanoptaServer.Status.valueOf(server.status.toUpperCase());
        Instant agentLastSynced = (server.agentLastSynced == null)
                ? null
                : Instant.from(PANOPTA_DATE_FORMAT.parse(server.agentLastSynced + " UTC"));
        return new PanoptaServer(partnerCustomerKey, serverId, server.serverKey, server.name, server.fqdn,
                                 server.serverGroup, status, agentLastSynced);
    }

    @Override
    public void pauseServerMonitoring(UUID vmId) {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            return;
        }
        long serverId = panoptaDetail.getServerId();
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
        long serverId = panoptaDetail.getServerId();
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
    public String getDefaultGroup(String shopperId) throws PanoptaServiceException {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        PanoptaServerGroupList groupList = panoptaApiServerGroupService.getServerGroups(partnerCustomerKey);
        return groupList.groups
                .stream()
                .filter(g -> g.name.equals("Default Server Group"))
                .findFirst()
                .orElseThrow(() -> new PanoptaServiceException(
                        "NO_SERVER_GROUP_FOUND",
                        "No default server group found."
                )).url;
    }
}
