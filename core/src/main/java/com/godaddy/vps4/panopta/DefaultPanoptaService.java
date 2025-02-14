package com.godaddy.vps4.panopta;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
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
    private static final int NETWORK_METRIC_FREQUENCY_SELF_MANAGED = 300;
    private static final int OUTAGE_CONFIRMATION_DELAY = 300;
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
    public String getStatus(String shopperId) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        PanoptaApiCustomerList panoptaApiCustomerList = panoptaApiCustomerService.getCustomer(partnerCustomerKey);
        if (!panoptaApiCustomerList.getCustomerList().isEmpty()) {
            Optional<PanoptaApiCustomerList.Customer> customer = panoptaApiCustomerList.getCustomerList().stream().findFirst();
            if (customer.isPresent()) {
                return customer.get().status;
            }
        }
        logger.info("Could not find customer in panopta for partner-customer-key {} ", partnerCustomerKey);
        return null;
    }

    @Override
    public void setStatus(String shopperId, String status) {
        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(shopperId);

        String emailAddress = config.get("panopta.api.customer.email");
        String panoptaNamePrefix = config.get("panopta.api.name.prefix");
        String panoptaPackage = config.get("panopta.api.package");

        PanoptaApiUpdateCustomerRequest panoptaApiUpdateCustomerRequest = new PanoptaApiUpdateCustomerRequest();
        panoptaApiUpdateCustomerRequest.setEmailAddress(emailAddress);
        panoptaApiUpdateCustomerRequest.setName(panoptaNamePrefix + shopperId);
        panoptaApiUpdateCustomerRequest.setPanoptaPackage(panoptaPackage);
        panoptaApiUpdateCustomerRequest.setStatus(status);
        logger.info("Update Panopta customer Request: {}", panoptaApiUpdateCustomerRequest);

        panoptaApiCustomerService.updateCustomer(panoptaCustomerDetails.getCustomerKey(), panoptaApiUpdateCustomerRequest);
    }

    @Override
    public PanoptaServer createServer(String shopperId,
                                      UUID orionGuid,
                                      String ipAddress,
                                      String[] tags) throws PanoptaServiceException {
        PanoptaApiServerRequest request = new PanoptaApiServerRequest(ipAddress, orionGuid.toString(),
                                                                      getDefaultGroup(shopperId), tags);

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
    public void applyTemplates(long serverId, String partnerCustomerKey, String[] templates) {
            for (String template : templates) {
                logger.info("Applying template {} to serverId: {}", template, serverId);
                PanoptaApiApplyTemplateRequest request = new PanoptaApiApplyTemplateRequest(template, true);
                panoptaApiServerService.applyTemplate(serverId, partnerCustomerKey, request);
            }
    }

    @Override
    public void removeTemplate(long serverId, String partnerCustomerKey, String templateId, String strategy) {
        logger.info("Removing templateId {} from serverId: {}", templateId, serverId);
        PanoptaApiRemoveTemplateRequest request = new PanoptaApiRemoveTemplateRequest(strategy);
        panoptaApiServerService.removeTemplate(serverId, partnerCustomerKey, templateId, request);
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
        PanoptaDetail serverInDb = panoptaDataService.getPanoptaDetails(vmId);
        if (serverInDb != null) {
            long serverId = serverInDb.getServerId();
            String partnerCustomerKey = serverInDb.getPartnerCustomerKey();
            PanoptaServers.Server serverInPanopta = null;
            try {
                serverInPanopta = panoptaApiServerService.getServer(serverId, partnerCustomerKey);
            } catch (NotFoundException e) {
                logger.info("Could not find server in Panopta for VM ID {} ", vmId);
                return null;
            }
            return mapServer(partnerCustomerKey, serverInPanopta);
        }
        logger.info("Could not find server in database for VM ID {} ", vmId);
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
    public void deleteServer(UUID vmId, String shopperId) throws PanoptaServiceException {
        validateAndGetOrCreatePanoptaCustomer(shopperId);
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
    public void addNetworkService(UUID vmId, VmMetric metric, String additionalFqdn, int osTypeId, boolean isManaged) throws PanoptaServiceException {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        int networkMetricFrequency = isManaged ? NETWORK_METRIC_FREQUENCY_MANAGED : NETWORK_METRIC_FREQUENCY_SELF_MANAGED;

        String name;
        int port;
        PanoptaApiNetworkServiceRequest.Metadata metadata;

        if (metric.equals(VmMetric.HTTPS_DOMAIN)) {
            name = "vps4_domain_monitoring_https";
            port = HTTPS_PORT;
            metadata = new PanoptaApiNetworkServiceRequest.HttpsMetadata(METRIC_OVERRIDE,
                                                                         SSL_EXPIRATION_WARNING_TIME,
                                                                         SSL_IGNORE);
        } else if (metric.equals(VmMetric.HTTP_DOMAIN)) {
            name = "vps4_domain_monitoring_http";
            port = HTTP_PORT;
            metadata = new PanoptaApiNetworkServiceRequest.Metadata(METRIC_OVERRIDE);
        } else {
            throw new PanoptaServiceException("UNKNOWN_METRIC", "Only acceptable metrics are "
                    + VmMetric.HTTP_DOMAIN + " or " + VmMetric.HTTPS_DOMAIN
                    + ". This metric is unknown: " + metric);
        }

        PanoptaApiNetworkServiceRequest request = new PanoptaApiNetworkServiceRequest(
                panoptaMetricMapper.getMetricTypeId(metric, osTypeId), networkMetricFrequency, name,
                EXCLUDE_FROM_AVAILABILITY, OUTAGE_CONFIRMATION_DELAY, port, additionalFqdn, metadata);
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
                    .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED)
                    .value;
            ids = ids.stream()
                    .filter(id -> panoptaMetricMapper.getVmMetric(id.typeId) != VmMetric.UNKNOWN)
                     .collect(Collectors.toList());
        }
        return ids;
    }

    @Override
    public List<PanoptaDomain> getAdditionalDomains(UUID vmId) {
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail == null) {
            return null;
        }
        Map<String, Instant> fqdnValidOnMap = panoptaDataService.getPanoptaAdditionalFqdnWithValidOn(vmId);
        return panoptaApiServerService
                .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED).value
                .stream()
                .filter(t -> Arrays.asList(VmMetric.HTTP_DOMAIN, VmMetric.HTTPS_DOMAIN)
                                   .contains(panoptaMetricMapper.getVmMetric(t.typeId)))
                .map(id -> new PanoptaDomain(id,
                                             panoptaMetricMapper.getVmMetric(id.typeId),
                                             fqdnValidOnMap.get(id.serverInterface)))
                .collect(Collectors.toList());
    }

    @Override
    public PanoptaMetricId getNetworkIdOfAdditionalFqdn(UUID vmId, String fqdn) throws PanoptaServiceException {
        List<PanoptaMetricId> ids = new ArrayList<>();
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        if (detail != null) {
            ids = panoptaApiServerService
                    .getNetworkList(detail.getServerId(), detail.getPartnerCustomerKey(), UNLIMITED)
                    .value;
            ids = ids.stream()
                    .filter(id -> (Arrays.asList(VmMetric.HTTP_DOMAIN, VmMetric.HTTPS_DOMAIN)).contains(panoptaMetricMapper.getVmMetric(id.typeId))
                                    && id.serverInterface.equals(fqdn))
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

    @Override
    public String getPartnerCustomerKey(String shopperId) {
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
    public void pauseServerMonitoring(UUID vmId, String shopperId) throws PanoptaServiceException {
        validateAndGetOrCreatePanoptaCustomer(shopperId);
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
    public List<VmOutage> getOutages(UUID vmId, Integer daysAgo, VmMetric metric, VmOutage.Status status) throws PanoptaServiceException {
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(vmId);
        if (panoptaDetail == null) {
            logger.warn("Could not find Panopta data for VM ID: {}", vmId);
            throw new PanoptaServiceException("NO_SERVER_FOUND",
                                              "No matching server found in VPS4 Panopta database for VM ID: " + vmId);
        }

        List<PanoptaMetricId> allMetricIds = getAllMetricIds(panoptaDetail);
        String statusStr = status != null ? status.toString().toLowerCase() : null;
        String daysAgoStr = daysAgo != null ? Instant.now().minus(daysAgo, ChronoUnit.DAYS).toString() : null;
        PanoptaOutageList outageList = panoptaApiServerService.getOutages(panoptaDetail.getServerId(),
                panoptaDetail.getPartnerCustomerKey(), statusStr, UNLIMITED, daysAgoStr);

        List<VmOutage> mappedOutages = outageList.value.stream()
                .map(outage -> mapPanoptaOutageToVmOutage(vmId, allMetricIds, outage))
                .collect(Collectors.toList());

        return mappedOutages.stream().filter(outage -> metric == null || outage.metrics.contains(metric))
                .collect(Collectors.toList());
    }

    private List<PanoptaMetricId> getAllMetricIds(PanoptaDetail panoptaDetail) {
        List<PanoptaMetricId> metricIds = new ArrayList<>();
        metricIds.addAll(panoptaApiServerService.getUsageList(panoptaDetail.getServerId(),
                                                              panoptaDetail.getPartnerCustomerKey(),
                                                              UNLIMITED).value);
        metricIds.addAll(panoptaApiServerService.getNetworkList(panoptaDetail.getServerId(),
                                                                panoptaDetail.getPartnerCustomerKey(),
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
        vmOutage.severity = outage.severity;
        vmOutage.status = outage.status;

        vmOutage.domainMonitoringMetadata = allMetricIds.stream()
                .filter(p -> outage.networkMetricMetadata.containsKey(p.id)
                        && (panoptaMetricMapper.getVmMetric(p.typeId) == VmMetric.HTTP_DOMAIN
                        || panoptaMetricMapper.getVmMetric(p.typeId) == VmMetric.HTTPS_DOMAIN))
                .map(p -> new VmOutage.DomainMonitoringMetadata(p.serverInterface, outage.networkMetricMetadata.get(p.id),
                        panoptaMetricMapper.getVmMetric(p.typeId)))
                .collect(Collectors.toList());

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

    @Override
    public Set<String> getOutageMetrics(UUID vmId) throws PanoptaServiceException {
        List<VmOutage> outages = this.getOutages(vmId, null, null, null);
        Set<String> outageMetrics = new HashSet<>();
        for (VmOutage outage : outages) {
            for (VmMetric metric : outage.metrics) {
                outageMetrics.add(metric.toString());
            }
        }
        return outageMetrics;
    }

    @Override
    public PanoptaCustomerDetails validateAndGetOrCreatePanoptaCustomer(String shopperId) throws PanoptaServiceException {
        PanoptaCustomerDetails customerInDb = panoptaDataService.getPanoptaCustomerDetails(shopperId);
        PanoptaCustomer customerInPanopta = getCustomer(shopperId);

        if (customerInPanopta == null) {
            if (customerInDb != null) {
                logger.info("Panopta customer is not active. Cleaning up customer and servers from DB.");
                destroyPanoptaCustomerAndServersInDb(shopperId);
            }
            customerInPanopta = createCustomer(shopperId);
            customerInDb = createAndGetCustomerInDb(shopperId, customerInPanopta.customerKey);
        } else {
            if (customerInDb != null && !customerInPanopta.customerKey.equals(customerInDb.getCustomerKey())) {
                logger.info("Panopta customer is out of sync. Re-creating customer with latest Panopta data.");
                destroyPanoptaCustomerAndServersInDb(shopperId);
                customerInDb = createAndGetCustomerInDb(shopperId, customerInPanopta.customerKey);
            } else if (customerInDb == null) {
                logger.info("Panopta customer is not present in this DB.");
                customerInDb = createAndGetCustomerInDb(shopperId, customerInPanopta.customerKey);
            }
        }
        return customerInDb;
    }

    private void destroyPanoptaCustomerAndServersInDb(String shopperId) {
        panoptaDataService.setAllPanoptaServersOfCustomerDestroyed(shopperId);
        panoptaDataService.checkAndSetPanoptaCustomerDestroyed(shopperId);
    }

    private PanoptaCustomerDetails createAndGetCustomerInDb(String shopperId, String customerKey) {
        panoptaDataService.createOrUpdatePanoptaCustomer(shopperId, customerKey);
        return panoptaDataService.getPanoptaCustomerDetails(shopperId);
    }
}
