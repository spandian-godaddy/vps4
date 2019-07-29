package com.godaddy.vps4.panopta;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PanoptaService {
    private static final Logger logger = LoggerFactory.getLogger(PanoptaService.class);
    private final PanoptaApiCustomerService panoptaApiCustomerService;
    private final PanoptaApiServerService panoptaApiServerService;

    @Inject
    public PanoptaService(PanoptaApiCustomerService panoptaApiCustomerService, PanoptaApiServerService panoptaApiServerService) {
        this.panoptaApiCustomerService = panoptaApiCustomerService;
        this.panoptaApiServerService = panoptaApiServerService;
    }

    public PanoptaCustomer createPanoptaCustomer(PanoptaCustomerRequest panoptaCustomerRequest)
            throws PanoptaServiceException {

        // setup the customer request for panopta
        PanoptaApiCustomerRequest panoptaApiCustomerRequest = new PanoptaApiCustomerRequest();
        panoptaApiCustomerRequest.panoptaPackage = panoptaCustomerRequest.getPanoptaPackage();
        panoptaApiCustomerRequest.name = panoptaCustomerRequest.getShopperId();
        panoptaApiCustomerRequest.emailAddress = panoptaCustomerRequest.getEmailAddress();
        panoptaApiCustomerRequest.partnerCustomerKey = panoptaCustomerRequest.getPartnerCustomerKey();

        // perform a POST to create the customer
        panoptaApiCustomerService.createCustomer(panoptaApiCustomerRequest);

        return mapResponseToPanoptaCustomer(
                getCustomerDetailsFromPanopta(panoptaCustomerRequest.getPartnerCustomerKey()));

    }

    private PanoptaCustomer mapResponseToPanoptaCustomer(PanoptaApiCustomerList.Customer customer) {
        return new PanoptaCustomer(customer.customerKey, customer.partnerCustomerKey);
    }

    private PanoptaApiCustomerList.Customer getCustomerDetailsFromPanopta(String partnerCustomerKey)
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

    public void deletePanoptaCustomer(String partnerCustomerKey) throws PanoptaServiceException {
        PanoptaCustomer panoptaCustomer =
                mapResponseToPanoptaCustomer(getCustomerDetailsFromPanopta(partnerCustomerKey));
        panoptaApiCustomerService.deleteCustomer(panoptaCustomer.customerKey);
    }

    public PanoptaServerMetric getServerMetricsFromPanopta(int agentResourceId, int serverId, String timescale, String partnerCustomerKey)
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

    public Map<String,Integer> getAgentResourceIdListFromPanopta(int serverId, String partnerCustomerKey)
            throws PanoptaServiceException {
        try {
            PanoptaAgentResourceList serverAgentResourceList = panoptaApiServerService.getAgentResourceList(serverId,partnerCustomerKey);
            return serverAgentResourceList.returnAgentResourceIdList();
        } catch (Exception ex) {
            String errorMessage = "Failed to get server's agent resource list in Panopta.";
            logger.error(errorMessage);
            throw new PanoptaServiceException("GET_AGENT_RESOURCE_FAILED", errorMessage);
        }
    }

}

