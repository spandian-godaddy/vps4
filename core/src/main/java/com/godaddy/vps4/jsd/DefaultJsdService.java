package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueCommentRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueRequest;
import com.godaddy.vps4.jsd.model.JsdApiSearchIssueRequest;
import com.godaddy.vps4.jsd.model.JsdContentDoc;
import com.godaddy.vps4.jsd.model.JsdContentNodeLabel;
import com.godaddy.vps4.jsd.model.JsdContentNodeValue;
import com.godaddy.vps4.jsd.model.JsdContentParagraph;
import com.godaddy.vps4.jsd.model.JsdCreatedComment;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.model.JsdFieldId;
import com.godaddy.vps4.jsd.model.JsdFieldKey;
import com.godaddy.vps4.jsd.model.JsdFieldName;
import com.godaddy.vps4.jsd.model.JsdFieldValue;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import com.godaddy.hfs.config.Config;

public class DefaultJsdService implements JsdService {
    private static final String REQUEST_TYPE = "Special Request";
    private static final String SERVICE_REQUEST_CATEGORY = "Monitoring Event";
    private static final String SUPPORT_TIER_LEVEL = "Tier 3";
    private final String timezoneForDateParams;
    private final String dateTimePattern;
    private final JsdApiService jsdApiService;
    private final Config config;
    private static final Logger logger = LoggerFactory.getLogger(DefaultJsdService.class);


    @Inject
    public DefaultJsdService(JsdApiService jsdApiService, Config config) {
        this.jsdApiService = jsdApiService;
        this.config = config;
        this.timezoneForDateParams = config.get("messaging.timezone");
        this.dateTimePattern = config.get("messaging.datetime.pattern");
    }

    @Override
    public JsdCreatedIssue createTicket(CreateJsdTicketRequest createJsdTicketRequest) {
        JsdApiIssueRequest.JsdIssueFields fields = buildRequestFields(createJsdTicketRequest);
        JsdApiIssueRequest jsdApiIssueRequest = new JsdApiIssueRequest(fields);
        try {
            return jsdApiService.createTicket(jsdApiIssueRequest);
        }
        catch (Exception e) {
            logger.error("Error creating JSD ticket for GUID {} : Exception :", jsdApiIssueRequest.fields.orionGuid, e);
            throw e;
        }
    }

    @Override
    public JsdIssueSearchResult searchTicket(String primaryIpAddress, Long outageId, UUID orionId) {
        String jql = "\"IP Address[Short text]\"~\"" + primaryIpAddress +
                "\" AND \"Outage ID[Short text]\"~\"" + outageId +
                "\" AND \"GUID[Short text]\"~\"" + orionId + "\"";

        String[] fields = new String[]{"summary", "id", "key"};
        JsdApiSearchIssueRequest jsdApiSearchIssueRequest = new JsdApiSearchIssueRequest(jql, 1, 0, fields);

        try {
            return jsdApiService.searchTicket(jsdApiSearchIssueRequest);
        }
        catch (Exception e) {
            logger.error("Error searching for JSD ticket for outageId {} : Exception :", outageId, e);
            throw e;
        }
    }

    @Override
    public JsdCreatedComment commentTicket(String ticketIdOrKey, String fqdn, String items, Instant timestamp) {
        String contentText = String.format("Outage has been cleared for: \nFQDN: %s\nItems: %s\nTimestamp: %s %s",
                                           fqdn, items, formatDateTime(timestamp), timezoneForDateParams);
        JsdContentNodeValue contentNode = new JsdContentNodeValue(contentText);
        JsdContentParagraph paragraph = new JsdContentParagraph(Collections.singletonList(contentNode));
        JsdContentDoc body = new JsdContentDoc(Collections.singletonList(paragraph));
        JsdApiIssueCommentRequest req = new JsdApiIssueCommentRequest(body);
        try {
            return jsdApiService.commentTicket(ticketIdOrKey, req);
        } catch (Exception e) {
            logger.error("Error commenting on for JSD ticket {} : Exception :", ticketIdOrKey, e);
            throw e;
        }
    }

    private String formatDateTime(Instant dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of(this.timezoneForDateParams));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(this.dateTimePattern);
        return dateTimeFormatter.format(zonedDateTime);
    }

    private JsdApiIssueRequest.JsdIssueFields buildRequestFields(CreateJsdTicketRequest createTicketRequest) {
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

        JsdContentDoc servicesAffected = buildServicesAffectedContent(createTicketRequest.metricTypes);

        JsdContentParagraph contentFqdn = buildDescriptionContent("FQDN: ", createTicketRequest.fqdn);

        JsdContentParagraph contentItems = buildDescriptionContent("Items: ", createTicketRequest.metricInfo);

        JsdContentParagraph contentReasons = buildDescriptionContent("Reasons: ", createTicketRequest.metricReasons);

        JsdContentDoc description = new JsdContentDoc(Arrays.asList(contentFqdn, contentItems, contentReasons));

        JsdApiIssueRequest.JsdIssueFields fields = new JsdApiIssueRequest.JsdIssueFields();

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

    private JsdContentParagraph buildDescriptionContent(String text, String value) {
        JsdContentNodeLabel contentNodeText = new JsdContentNodeLabel(text);

        JsdContentNodeValue contentNodeValue = new JsdContentNodeValue(value);

        JsdContentParagraph content = new JsdContentParagraph(Arrays.asList(contentNodeText, contentNodeValue));

        return content;
    }

    private JsdContentDoc buildServicesAffectedContent(String servicesAffected) {
        JsdContentNodeLabel contentNode = new JsdContentNodeLabel(servicesAffected);
        JsdContentParagraph content = new JsdContentParagraph(Collections.singletonList(contentNode));
        JsdContentDoc servicesAffectedContent = new JsdContentDoc(Collections.singletonList(content));
        return servicesAffectedContent;
    }
}
