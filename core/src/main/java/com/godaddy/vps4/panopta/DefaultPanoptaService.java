package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;

public class DefaultPanoptaService implements PanoptaService {
    private static final DateTimeFormatter PANOPTA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final int UNLIMITED = 0;
    private static final int SLEEP_SECONDS = 10;
    private static final int TIMEOUT_MINUTES = 1;
    private static final int NETWORK_METRIC_FREQUENCY_MANAGED = 60;
    private static final int OUTAGE_CONFIRMATION_DELAY_MANAGED = 900;
    private static final int NETWORK_METRIC_FREQUENCY_SELF_MANAGED = 300;
    private static final int OUTAGE_CONFIRMATION_DELAY_SELF_MANAGED = 300;
    private static final int HTTPS_PORT = 443;
    private static final int HTTP_PORT = 80;
    private static final String SSL_EXPIRATION_WARNING_TIME = "14";
    private static final String SSL_IGNORE = "off";

    private static final boolean METRIC_OVERRIDE = false;
    private static final boolean EXCLUDE_FROM_AVAILABILITY = true;

    private static final Logger logger = LoggerFactory.getLogger(DefaultPanoptaService.class);
    private final Cache<String, CachedMonitoringGraphs> cache;
    private final ExecutorService pool;
    private final PanoptaApiCustomerService panoptaApiCustomerService;
    private final PanoptaApiServerService panoptaApiServerService;
    private final PanoptaApiServerGroupService panoptaApiServerGroupService;
    private final PanoptaApiOutageService panoptaApiOutageService;
    private final PanoptaDataService panoptaDataService;
    private final Config config;
    private final PanoptaMetricMapper panoptaMetricMapper;

    @Inject
    public DefaultPanoptaService(@PanoptaExecutorService ExecutorService pool,
                                 CacheManager cacheManager,
                                 PanoptaApiCustomerService panoptaApiCustomerService,
                                 PanoptaApiServerService panoptaApiServerService,
                                 PanoptaApiServerGroupService panoptaApiServerGroupService,
                                 PanoptaApiOutageService panoptaApiOutageService,
                                 PanoptaDataService panoptaDataService,
                                 Config config,
                                 PanoptaMetricMapper panoptaMetricMapper) {
        this.cache = cacheManager.getCache(CacheName.PANOPTA_METRIC_GRAPH,
                                           String.class,
                                           CachedMonitoringGraphs.class);
        this.pool = pool;
        this.panoptaApiCustomerService = panoptaApiCustomerService;
        this.panoptaApiServerService = panoptaApiServerService;
        this.panoptaApiServerGroupService = panoptaApiServerGroupService;
        this.panoptaApiOutageService = panoptaApiOutageService;
        this.panoptaDataService = panoptaDataService;
        this.config = config;
        this.panoptaMetricMapper = panoptaMetricMapper;
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
                panoptaApiCustomerService.getCustomersByStatus(partnerCustomerKey, "active");
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
    public void deleteAdditionalFqdnFromServer(UUID vmId, String additionalFqdn) throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                    "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        long serverId = panoptaDetail.getServerId();
        String partnerCustomerKey = panoptaDetail.getPartnerCustomerKey();
        PanoptaServer server =
                mapServer(partnerCustomerKey, panoptaApiServerService.getServer(serverId, partnerCustomerKey));

        server.additionalFqdns = server.additionalFqdns.stream()
                .filter(fqdn -> !fqdn.equals(additionalFqdn))
                .collect(Collectors.toList());

        PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest = new PanoptaApiUpdateServerRequest();
        panoptaApiUpdateServerRequest.fqdn = server.fqdn;
        panoptaApiUpdateServerRequest.name = server.name;
        panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
        panoptaApiUpdateServerRequest.additionalFqdns = server.additionalFqdns;

        panoptaApiServerService.updateServer(serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
    }

    @Override
    public void addAdditionalFqdnToServer(UUID vmId, String additionalFqdn) throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                    "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        long serverId = panoptaDetail.getServerId();
        String partnerCustomerKey = panoptaDetail.getPartnerCustomerKey();
        PanoptaServer server =
                mapServer(partnerCustomerKey, panoptaApiServerService.getServer(serverId, partnerCustomerKey));

        List<String> additionalFqdns = new ArrayList<>(server.additionalFqdns);
        additionalFqdns.add(additionalFqdn);

        PanoptaApiUpdateServerRequest panoptaApiUpdateServerRequest = new PanoptaApiUpdateServerRequest();
        panoptaApiUpdateServerRequest.fqdn = server.fqdn;
        panoptaApiUpdateServerRequest.name = server.name;
        panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
        panoptaApiUpdateServerRequest.additionalFqdns = additionalFqdns;
        panoptaApiServerService.updateServer(serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
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
                                                               orionGuid.toString(),
                                                               "active");
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
    public void addNetworkService(UUID vmId, VmMetric metric, String additionalFqdn, int osTypeId, boolean isManaged, boolean hasMonitoring) throws PanoptaServiceException {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        PanoptaApiNetworkServiceRequest request;
        int port;
        int networkMetricFrequency = isManaged ? NETWORK_METRIC_FREQUENCY_MANAGED : NETWORK_METRIC_FREQUENCY_SELF_MANAGED;
        int outageConfirmationDelay = isManaged ? OUTAGE_CONFIRMATION_DELAY_MANAGED : (hasMonitoring ? OUTAGE_CONFIRMATION_DELAY_MANAGED : OUTAGE_CONFIRMATION_DELAY_SELF_MANAGED);
        PanoptaApiNetworkServiceRequest.Metadata metadata = new PanoptaApiNetworkServiceRequest.Metadata();
        PanoptaApiNetworkServiceRequest.HttpsMetadata httpsMetadata = new PanoptaApiNetworkServiceRequest.HttpsMetadata();
        if(metric.equals(VmMetric.HTTPS)) {
            httpsMetadata = new PanoptaApiNetworkServiceRequest.HttpsMetadata();
            httpsMetadata.metricOverride = METRIC_OVERRIDE;
            httpsMetadata.httpSslExpiration = SSL_EXPIRATION_WARNING_TIME;
            httpsMetadata.httpSslIgnore = SSL_IGNORE;
            port = HTTPS_PORT;
        }
        else if (metric.equals(VmMetric.HTTP)) {
            metadata = new PanoptaApiNetworkServiceRequest.Metadata();
            metadata.metricOverride = METRIC_OVERRIDE;
            port = HTTP_PORT;
        }
        else {
            throw new PanoptaServiceException("UNKNOWN_METRIC", "Only acceptable metrics is HTTP or HTTPS. This metric is unknown: "+ metric);
        }

        request = new PanoptaApiNetworkServiceRequest(panoptaMetricMapper.getMetricTypeId(metric, osTypeId),
                networkMetricFrequency, EXCLUDE_FROM_AVAILABILITY, outageConfirmationDelay, port, additionalFqdn,
                metric.equals(VmMetric.HTTPS) ? httpsMetadata : metadata);
        panoptaApiServerService.addNetworkService(panoptaDetails.getServerId(),
                panoptaDetails.getPartnerCustomerKey(),
                request);
    }


    @Override
    public void deleteNetworkService(UUID vmId, long networkServiceId) throws PanoptaServiceException {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetails == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                    "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }

        panoptaApiServerService.deleteNetworkService(panoptaDetails.getServerId(),
                networkServiceId, panoptaDetails.getPartnerCustomerKey());
    }

    @Override
    public List<PanoptaMetricId> getUsageIds(UUID vmId) {
        List<PanoptaMetricId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getUsageList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED)
                    .value;
            ids = ids.stream()
                     .filter(id -> panoptaMetricMapper.getVmMetric(id.typeId) != VmMetric.UNKNOWN)
                     .collect(Collectors.toList());
        }
        return ids;
    }

    @Override
    public List<PanoptaMetricId> getNetworkIds(UUID vmId) {
        List<PanoptaMetricId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(),null, UNLIMITED)
                    .value;
            ids = ids.stream()
                    .filter(id -> panoptaMetricMapper.getVmMetric(id.typeId) != VmMetric.UNKNOWN)
                     .collect(Collectors.toList());
        }
        return ids;
    }

    @Override
    public List<PanoptaMetricId> getAdditionalFqdnMetricIds(UUID vmId) {
        List<PanoptaMetricId> ids = new ArrayList<>();
        List<String> additionalFqdns = panoptaDataService.getPanoptaActiveAdditionalFqdns(vmId);
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                            .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(), null, UNLIMITED)
                            .value;
            ids = ids.stream().filter(t -> additionalFqdns.contains(t.serverInterface) &&
                    (Arrays.asList(VmMetric.HTTP, VmMetric.HTTPS)).contains(panoptaMetricMapper.getVmMetric(t.typeId)))
                    .collect(Collectors.toList());
        }
        return ids;
    }

    @Override
    public PanoptaMetricId getNetworkIdOfAdditionalFqdn(UUID vmId, String fqdn) throws PanoptaServiceException {
        List<PanoptaMetricId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(), fqdn, UNLIMITED)
                    .value;
            ids = ids.stream()
                    .filter(id -> (Arrays.asList(VmMetric.HTTP, VmMetric.HTTPS)).contains(panoptaMetricMapper.getVmMetric(id.typeId)))
                    .collect(Collectors.toList());
        }
        if (ids.isEmpty()) {
            throw new PanoptaServiceException("NO_NETWORK_ID_FOUND",
                    "No matching network Id found in Panopta for fqdn: " + fqdn + " and VM ID: " + vmId);
        }
        return ids.get(0);
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
            List<PanoptaMetricId> usageIds = getUsageIds(vmId);
            List<Callable<PanoptaGraph>> tasks = new ArrayList<>();
            for (PanoptaMetricId usageId : usageIds) {
                Callable<PanoptaGraph> task = () -> {
                    PanoptaGraph graph = panoptaApiServerService.getUsageGraph(detail.getServerId(),
                                                                               usageId.id,
                                                                               timescale,
                                                                               detail.getPartnerCustomerKey());
                    graph.type = panoptaMetricMapper.getVmMetric(usageId.typeId);
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
            List<PanoptaMetricId> networkIds = getNetworkIds(vmId);
            List<Callable<PanoptaGraph>> tasks = new ArrayList<>();
            for (PanoptaMetricId networkId : networkIds) {
                Callable<PanoptaGraph> task = () -> {
                    PanoptaGraph graph = panoptaApiServerService.getNetworkGraph(detail.getServerId(),
                                                                                 networkId.id,
                                                                                 timescale,
                                                                                 detail.getPartnerCustomerKey());
                    graph.type = panoptaMetricMapper.getVmMetric(networkId.typeId);
                    graph.metadata = networkId.metadata;
                    graph.serverInterface = networkId.serverInterface;
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
                                    server.additionalFqdns, server.serverGroup, status, agentLastSynced);
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
            panoptaApiServerService.updateServer(serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
        } else {
            logger.info("Panopta server is already in suspended status. No need to update status");
        }
    }

    @Override
    public void resumeServerMonitoring(UUID vmId, UUID orionGuid) {
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
            panoptaApiUpdateServerRequest.name = orionGuid.toString();
            panoptaApiUpdateServerRequest.serverGroup = server.serverGroup;
            panoptaApiUpdateServerRequest.status = PanoptaServer.Status.ACTIVE.toString().toLowerCase();
            logger.info("Setting Panopta server to active status");
            panoptaApiServerService.updateServer(serverId, partnerCustomerKey, panoptaApiUpdateServerRequest);
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
    public VmOutage getOutage(UUID vmId, long outageId) throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }
        try {
            PanoptaOutage outage = panoptaApiOutageService.getOutage(outageId, panoptaDetail.getPartnerCustomerKey());
            List<PanoptaMetricId> allMetricIds = getAllMetricIds(panoptaDetail);
            return mapPanoptaOutageToVmOutage(vmId, allMetricIds, outage);
        } catch (NotFoundException ignored) {
            throw new PanoptaServiceException("NO_OUTAGE_FOUND",
                                              "No matching outage found for VM ID: " + vmId);
        }
    }

    @Override
    public List<VmOutage> getOutages(UUID vmId, boolean activeOnly) throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }

        List<PanoptaMetricId> allMetricIds = getAllMetricIds(panoptaDetail);
        PanoptaOutageList outageList = panoptaApiServerService.getOutages(panoptaDetail.getServerId(),
                                                                          panoptaDetail.getPartnerCustomerKey(),
                                                                          (activeOnly) ? "active" : null,
                                                                          UNLIMITED);

        return outageList.value.stream()
                               .map(outage -> mapPanoptaOutageToVmOutage(vmId, allMetricIds, outage))
                               .collect(Collectors.toList());
    }

    private List<PanoptaMetricId> getAllMetricIds(PanoptaDetail panoptaDetail) {
        List<PanoptaMetricId> metricIds = new ArrayList<>();
        metricIds.addAll(panoptaApiServerService.getUsageList(panoptaDetail.getServerId(),
                                                              panoptaDetail.getPartnerCustomerKey(),
                                                              UNLIMITED).value);
        metricIds.addAll(panoptaApiServerService.getNetworkList(panoptaDetail.getServerId(),
                                                                panoptaDetail.getPartnerCustomerKey(),
                                                                null,
                                                                UNLIMITED).value);
        return metricIds;
    }

    private VmOutage mapPanoptaOutageToVmOutage(UUID vmId, List<PanoptaMetricId> allMetricIds, PanoptaOutage outage) {
        VmOutage vmOutage = new VmOutage();
        vmOutage.vmId = vmId;
        vmOutage.started = outage.started;
        vmOutage.ended = outage.ended;
        vmOutage.reason = outage.reason;
        vmOutage.panoptaOutageId = outage.outageId;

        vmOutage.metrics = allMetricIds.stream()
                                       .filter(metricId -> outage.metricIds.contains(metricId.id))
                                       .map((metricId -> panoptaMetricMapper.getVmMetric(metricId.typeId)))
                                       .collect(Collectors.toSet());

        return vmOutage;
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
