package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueCommentRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueRequest;
import com.godaddy.vps4.jsd.model.JsdApiSearchIssueRequest;
import com.godaddy.vps4.jsd.model.JsdContentNodeLabel;
import com.godaddy.vps4.jsd.model.JsdContentNodeMarks;
import com.godaddy.vps4.jsd.model.JsdContentNodeValue;
import com.godaddy.vps4.jsd.model.JsdContentParagraph;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.godaddy.hfs.config.Config;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultJsdServiceTest {
    @Mock
    private JsdApiService jsdApiService;
    @Mock
    private Config config;

    private DefaultJsdService defaultJsdService;

    private ArgumentCaptor<JsdApiIssueRequest> requestArgument = ArgumentCaptor.forClass(JsdApiIssueRequest.class);
    private ArgumentCaptor<JsdApiIssueCommentRequest> commentArgument = ArgumentCaptor.forClass(JsdApiIssueCommentRequest.class);
    private ArgumentCaptor<JsdApiSearchIssueRequest> searchArgument = ArgumentCaptor.forClass(JsdApiSearchIssueRequest.class);


    private UUID orionGuid = UUID.randomUUID();
    private CreateJsdTicketRequest request = new CreateJsdTicketRequest();

    @Before
    public void setup() throws IOException {
        request.shopperId = "fake-shopper";
        request.summary = "Monitoring Event - Agent Heartbeat (0000000)";
        request.partnerCustomerKey = "partnerCustomerKey";
        request.severity = "standard";
        request.outageId = "1231231";
        request.metricTypes = "internal.agent.heartbeat";
        request.metricInfo = "Agent Heartbeat";
        request.metricReasons = "Agent Heartbeat (10.0.0.1)";
        request.fqdn = "10.0.0.1";
        request.orionGuid = orionGuid.toString();
        request.customerProduct = "Fully Managed";
        request.supportProduct = "vps4";
        request.dataCenter = "a2";
        request.outageIdUrl = "https://my.panopta.com/outage/manageIncident?incident_id=0000000";
        request.plid = "123456";

        when(config.get("jsd.project.key")).thenReturn("projectKEY");
        when(config.get("jsd.api.reporter.id")).thenReturn("reporterID");
        when(config.get("messaging.timezone")).thenReturn("GMT");
        when(config.get("messaging.datetime.pattern")).thenReturn("yyyy-MM-dd HH:mm:ss");

        defaultJsdService = new DefaultJsdService(jsdApiService, config);
    }

    @Test
    public void testInvokesCreatesTicketAPI() {
        CreateJsdTicketRequest request = new CreateJsdTicketRequest();
        defaultJsdService.createTicket(request);
        verify(jsdApiService).createTicket(any(JsdApiIssueRequest.class));
    }

    @Test(expected = Exception.class)
    public void testThrowsErrorIfCreatesTicketAPIErrors() {
        CreateJsdTicketRequest request = new CreateJsdTicketRequest();

        when(jsdApiService.createTicket(any(JsdApiIssueRequest.class))).thenThrow(new Exception());

        defaultJsdService.createTicket(request);
    }

    @Test
    public void passesCorrectParamsFromRequest() {
        defaultJsdService.createTicket(request);

        verify(jsdApiService).createTicket(requestArgument.capture());
        JsdApiIssueRequest jsdApiIssueRequest = requestArgument.getValue();

        assertEquals(orionGuid.toString(), jsdApiIssueRequest.fields.orionGuid);
        assertEquals(request.shopperId, jsdApiIssueRequest.fields.shopperId);
        assertEquals(request.summary, jsdApiIssueRequest.fields.summary);
        assertEquals(request.partnerCustomerKey, jsdApiIssueRequest.fields.partnerCustomerKey);
        assertEquals(request.plid, jsdApiIssueRequest.fields.plid);
        assertEquals(request.fqdn, jsdApiIssueRequest.fields.fqdn);
        assertEquals(request.severity, jsdApiIssueRequest.fields.severity.value);
        assertEquals(request.outageId, jsdApiIssueRequest.fields.outageId);
        assertEquals(request.outageIdUrl, jsdApiIssueRequest.fields.outageIdUrl);
        assertEquals(request.supportProduct, jsdApiIssueRequest.fields.supportProduct.value);
        assertEquals(request.customerProduct, jsdApiIssueRequest.fields.customerProduct.value);
        assertEquals(request.metricTypes, jsdApiIssueRequest.fields.metricTypes);
        assertEquals(request.dataCenter, jsdApiIssueRequest.fields.dataCenter.value);
        assertEquals(request.hypervisorHostname, jsdApiIssueRequest.fields.hypervisorHostname);
    }

    @Test
    public void passesCorrectConfigParams() {
        defaultJsdService.createTicket(request);

        verify(jsdApiService).createTicket(requestArgument.capture());
        JsdApiIssueRequest jsdApiIssueRequest = requestArgument.getValue();

        assertEquals("Special Request", jsdApiIssueRequest.fields.issueType.name);
        assertEquals("projectKEY", jsdApiIssueRequest.fields.project.key);
        assertEquals("reporterID", jsdApiIssueRequest.fields.reporter.id);
        assertEquals("Monitoring Event", jsdApiIssueRequest.fields.serviceRequestCategory.value);
        assertEquals("Tier 3", jsdApiIssueRequest.fields.supportTier.value);
        assertEquals("Fully Managed Server", jsdApiIssueRequest.fields.escalationReason.value);
        assertEquals("sysadmin", jsdApiIssueRequest.fields.emailIsc);
        assertEquals("Shopper ID and Participants", jsdApiIssueRequest.fields.emailTarget.value);
    }


    @Test
    public void passesCorrectDescription() {
        JsdContentNodeLabel contentNodeFqdnText, contentNodeItemsText, contentNodeReasonsText;
        JsdContentNodeValue contentNodeFqdnValue, contentNodeItemsValue, contentNodeReasonsValue;
        JsdContentNodeMarks contentMark;
        JsdContentParagraph contentFqdn, contentItems, contentReasons;
        defaultJsdService.createTicket(request);

        verify(jsdApiService).createTicket(requestArgument.capture());
        JsdApiIssueRequest jsdApiIssueRequest = requestArgument.getValue();

        contentFqdn = (JsdContentParagraph) jsdApiIssueRequest.fields.description.contentList.get(0);
        contentNodeFqdnText = (JsdContentNodeLabel) contentFqdn.contentList.get(0);
        contentNodeFqdnValue = (JsdContentNodeValue) contentFqdn.contentList.get(1);
        contentMark = (JsdContentNodeMarks) contentNodeFqdnText.marks.get(0);
        contentItems = (JsdContentParagraph) jsdApiIssueRequest.fields.description.contentList.get(1);
        contentNodeItemsText = (JsdContentNodeLabel) contentItems.contentList.get(0);
        contentNodeItemsValue = (JsdContentNodeValue) contentItems.contentList.get(1);
        contentReasons = (JsdContentParagraph) jsdApiIssueRequest.fields.description.contentList.get(2);
        contentNodeReasonsText = (JsdContentNodeLabel) contentReasons.contentList.get(0);
        contentNodeReasonsValue = (JsdContentNodeValue) contentReasons.contentList.get(1);

        assertEquals("doc", jsdApiIssueRequest.fields.description.type);
        assertEquals(new Integer(1), jsdApiIssueRequest.fields.description.version);
        assertEquals("strong",contentMark.type);
        assertEquals("FQDN: ", contentNodeFqdnText.text);
        assertEquals("10.0.0.1", contentNodeFqdnValue.text);
        assertEquals("Items: ", contentNodeItemsText.text);
        assertEquals("Agent Heartbeat", contentNodeItemsValue.text);
        assertEquals(    "Reasons: ", contentNodeReasonsText.text);
        assertEquals("Agent Heartbeat (10.0.0.1)", contentNodeReasonsValue.text);
    }

    @Test
    public void testInvokesSearchTicketAPI() {
        defaultJsdService.searchTicket(request.fqdn, Long.parseLong(request.outageId), orionGuid);
        verify(jsdApiService).searchTicket(any(JsdApiSearchIssueRequest.class));
    }

    @Test
    public void passesCorrectParamSearchTicket() {
        defaultJsdService.searchTicket(request.fqdn, Long.parseLong(request.outageId), orionGuid);

        verify(jsdApiService).searchTicket(searchArgument.capture());
        JsdApiSearchIssueRequest jsdApiSearchIssueRequest = searchArgument.getValue();

        String jql = "\"IP Address[Short text]\"~\"\\\"" + request.fqdn + "\\\"\" AND" +
                " \"Outage ID[Short text]\"~\"\\\"" + request.outageId + "\\\"\" AND" +
                " \"GUID[Short text]\"~\"\\\"" + request.orionGuid + "\\\"\"";
        assertEquals("summary", jsdApiSearchIssueRequest.fields[0]);
        assertEquals("id", jsdApiSearchIssueRequest.fields[1]);
        assertEquals("key", jsdApiSearchIssueRequest.fields[2]);
        assertEquals(jql, jsdApiSearchIssueRequest.jql);
        assertEquals(1, jsdApiSearchIssueRequest.maxResults);
        assertEquals(0, jsdApiSearchIssueRequest.startAt);
    }

    @Test(expected = Exception.class)
    public void testThrowsErrorIfSearchTicketAPIErrors() {
        when(jsdApiService.searchTicket(any(JsdApiSearchIssueRequest.class))).thenThrow(new Exception());

        defaultJsdService.searchTicket(request.fqdn, Long.parseLong(request.outageId), orionGuid);
    }

    @Test
    public void testInvokesCommentTicketAPI() {
        defaultJsdService.commentTicket("ticketId", request.fqdn,  request.metricInfo, Instant.now());
        verify(jsdApiService).commentTicket(eq("ticketId"), any(JsdApiIssueCommentRequest.class));
    }

    @Test
    public void passesCorrectParamCommentTicket() {
        Instant timestamp = Instant.now();
        
        defaultJsdService.commentTicket("ticketId", request.fqdn,  request.metricInfo, timestamp);

        verify(jsdApiService).commentTicket(eq("ticketId"), commentArgument.capture());
        JsdApiIssueCommentRequest jsdApiIssueCommentRequest = commentArgument.getValue();
        JsdContentParagraph contentParagraph = (JsdContentParagraph)jsdApiIssueCommentRequest.body.contentList.get(0);
        JsdContentNodeValue contentValue = (JsdContentNodeValue)contentParagraph.contentList.get(0);

        ZonedDateTime zonedDateTime = timestamp.atZone(ZoneId.of("GMT"));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time = dateTimeFormatter.format(zonedDateTime);

        String comment = "Outage has been cleared for: \n" +
                "FQDN: " + request.fqdn + "\n" +
                "Items: " + request.metricInfo + "\n" +
                "Timestamp: " + time + " GMT";

        assertEquals(new Integer(1), jsdApiIssueCommentRequest.body.version);
        assertEquals("doc", jsdApiIssueCommentRequest.body.type);
        assertEquals("paragraph", contentParagraph.type);
        assertEquals(comment, contentValue.text);
        assertEquals("text", contentValue.type);
        assertEquals("sd.public.comment", jsdApiIssueCommentRequest.commentProperties.get(0).key);
        assertEquals(true, jsdApiIssueCommentRequest.commentProperties.get(0).value.internal);
    }

    @Test(expected = Exception.class)
    public void testThrowsErrorIfCommentTicketAPIErrors() {
        when(jsdApiService.commentTicket(eq("ticketId"), any(JsdApiIssueCommentRequest.class))).thenThrow(new Exception());
        defaultJsdService.commentTicket("ticketId", request.fqdn,  request.metricInfo, Instant.now());
    }

}
