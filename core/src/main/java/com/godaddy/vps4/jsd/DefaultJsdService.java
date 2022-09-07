package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.model.JsdFieldId;
import com.godaddy.vps4.jsd.model.JsdFieldKey;
import com.godaddy.vps4.jsd.model.JsdFieldName;
import com.godaddy.vps4.jsd.model.JsdFieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import com.godaddy.hfs.config.Config;

public class DefaultJsdService implements JsdService {
    private static final String REQUEST_TYPE = "Special Request";
    private static final String SERVICE_REQUEST_CATEGORY = "Monitoring Event";
    private static final String SUPPORT_TIER_LEVEL = "Tier 3";

    private final JsdApiService jsdApiService;
    private final Config config;
    private static final Logger logger = LoggerFactory.getLogger(DefaultJsdService.class);


    @Inject
    public DefaultJsdService(JsdApiService jsdApiService, Config config) {
        this.jsdApiService = jsdApiService;
        this.config = config;
    }

    @Override
    public JsdCreatedIssue createTicket(CreateJsdTicketRequest createJSDTicketRequest) throws Exception {
        JsdApiIssueRequest.JSDIssueFields fields = buildRequestFields(createJSDTicketRequest);
        JsdApiIssueRequest jsdApiIssueRequest = new JsdApiIssueRequest(fields);
        logger.info("request here {}", jsdApiIssueRequest);
        try {
            return jsdApiService.createTicket(jsdApiIssueRequest);
        }
        catch (Exception e) {
            logger.error("Error creating JSD ticket for GUID {} : Exception :", jsdApiIssueRequest.fields.orionGuid, e);
            throw e;
        }
    }

    private JsdApiIssueRequest.JSDIssueFields buildRequestFields(CreateJsdTicketRequest createTicketRequest) {
        JsdFieldValue requestCategory, supportTier, severity, supportProduct, customerProduct, dataCenter;

        JsdFieldName issueType = new JsdFieldName(REQUEST_TYPE);
        JsdFieldKey project = new JsdFieldKey(config.get("jsd.project.key"));
        JsdFieldId reporter = new JsdFieldId(config.get("jsd.api.reporter.id"));

        requestCategory = new JsdFieldValue(SERVICE_REQUEST_CATEGORY);
        supportTier = new JsdFieldValue(SUPPORT_TIER_LEVEL);
        severity = new JsdFieldValue(createTicketRequest.severity);
        supportProduct = new JsdFieldValue(createTicketRequest.supportProduct);
        customerProduct = new JsdFieldValue(createTicketRequest.customerProduct);
        dataCenter = new JsdFieldValue(createTicketRequest.dataCenter);

        JsdApiIssueRequest.ContentDoc servicesAffected = buildServicesAffectedContent(createTicketRequest.metricTypes);

        JsdApiIssueRequest.ContentParagraph contentFqdn = buildDescriptionContent("FQDN: ", createTicketRequest.fqdn);

        JsdApiIssueRequest.ContentParagraph contentItems = buildDescriptionContent("Items: ", createTicketRequest.metricInfo);

        JsdApiIssueRequest.ContentParagraph contentReasons = buildDescriptionContent("Reasons: ", createTicketRequest.metricReasons);

        JsdApiIssueRequest.ContentDoc description =
                new JsdApiIssueRequest.ContentDoc(Arrays.asList(contentFqdn, contentItems, contentReasons));

        JsdApiIssueRequest.JSDIssueFields fields = new JsdApiIssueRequest.JSDIssueFields();

        fields.issueType = issueType;
        fields.project = project;
        fields.reporter = reporter;
        fields.summary = createTicketRequest.summary;
        fields.partnerCustomerKey = createTicketRequest.partnerCustomerKey;
        fields.plid = createTicketRequest.plid;
        fields.serviceRequestCategory = requestCategory;
        fields.fqdn = createTicketRequest.fqdn;
        fields.supportTier = supportTier;
        fields.severity = severity;
        fields.shopperId = createTicketRequest.shopperId;
        fields.outageId = createTicketRequest.outageId;
        fields.outageIdUrl = createTicketRequest.outageIdUrl;
        fields.supportProduct = supportProduct;
        fields.customerProduct = customerProduct;
        fields.orionGuid = createTicketRequest.orionGuid;
        fields.metricTypes = createTicketRequest.metricTypes;
        fields.servicesAffected = servicesAffected;
        fields.dataCenter = dataCenter;
        fields.description = description;

        return fields;
    }

    private JsdApiIssueRequest.ContentParagraph buildDescriptionContent(String text, String value) {
        JsdApiIssueRequest.ContentNodeLabel contentNodeText = new JsdApiIssueRequest.ContentNodeLabel(text);

        JsdApiIssueRequest.ContentNodeValue contentNodeValue = new JsdApiIssueRequest.ContentNodeValue(value);

        JsdApiIssueRequest.ContentParagraph content = new JsdApiIssueRequest.ContentParagraph(Arrays.asList(contentNodeText, contentNodeValue));

        return content;
    }

    private JsdApiIssueRequest.ContentDoc buildServicesAffectedContent(String servicesAffected) {
        JsdApiIssueRequest.ContentNodeLabel contentNode = new JsdApiIssueRequest.ContentNodeLabel(servicesAffected);
        JsdApiIssueRequest.ContentParagraph content = new JsdApiIssueRequest.ContentParagraph(Arrays.asList(contentNode));
        JsdApiIssueRequest.ContentDoc servicesAffectedContent = new JsdApiIssueRequest.ContentDoc(Arrays.asList(content));
        return servicesAffectedContent;
    }
}
