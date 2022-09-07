package com.godaddy.vps4.jsd;

import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdApiIssueRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.godaddy.hfs.config.Config;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultJsdServiceTest {
    @Mock
    private JsdApiService jsdApiService;
    @Mock
    private Config config;

    private DefaultJsdService defaultJSDService;

    private ArgumentCaptor<JsdApiIssueRequest> argument = ArgumentCaptor.forClass(JsdApiIssueRequest.class);

    private UUID orionGuid = UUID.randomUUID();
    private CreateJsdTicketRequest request = new CreateJsdTicketRequest();

    @Before
    public void setup() throws IOException {
        request.shopperId = "fake-shopper";
        request.summary = "Monitoring Event - Agent Heartbeat (0000000)";
        request.partnerCustomerKey = "partnerCustomerKey";
        request.severity = "standard";
        request.outageId = "0000000";
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

        defaultJSDService = new DefaultJsdService(jsdApiService, config);
        when(config.get("jsd.project.key")).thenReturn("projectKEY");
        when(config.get("jsd.api.reporter.id")).thenReturn("reporterID");
    }

    @Test
    public void testInvokesCreatesTicketAPI() throws Exception {
        CreateJsdTicketRequest request = new CreateJsdTicketRequest();
        defaultJSDService.createTicket(request);
        verify(jsdApiService).createTicket(any(JsdApiIssueRequest.class));
    }

    @Test
    public void testThrowsErrorIfCreatesTicketAPIErrors() throws Exception {
        CreateJsdTicketRequest request = new CreateJsdTicketRequest();

        when(jsdApiService.createTicket(any(JsdApiIssueRequest.class))).thenThrow(new Exception());

        try {
            defaultJSDService.createTicket(request);
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void passesCorrectParamsFromRequest() throws Exception {
        defaultJSDService.createTicket(request);

        verify(jsdApiService).createTicket(argument.capture());
        JsdApiIssueRequest jsdApiIssueRequest = argument.getValue();

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
    }

    @Test
    public void passesCorrectConfigParams() throws Exception {
        defaultJSDService.createTicket(request);

        verify(jsdApiService).createTicket(argument.capture());
        JsdApiIssueRequest jsdApiIssueRequest = argument.getValue();

        assertEquals("Special Request", jsdApiIssueRequest.fields.issueType.name);
        assertEquals("projectKEY", jsdApiIssueRequest.fields.project.key);
        assertEquals("reporterID", jsdApiIssueRequest.fields.reporter.id);
        assertEquals("Monitoring Event", jsdApiIssueRequest.fields.serviceRequestCategory.value);
        assertEquals("Tier 3", jsdApiIssueRequest.fields.supportTier.value);
    }


    @Test
    public void passesCorrectDescription() throws Exception {
        JsdApiIssueRequest.ContentNodeLabel contentNodeFqdnText, contentNodeItemsText, contentNodeReasonsText;
        JsdApiIssueRequest.ContentNodeValue contentNodeFqdnValue, contentNodeItemsValue, contentNodeReasonsValue;
        JsdApiIssueRequest.ContentNodeMarks contentMark;
        JsdApiIssueRequest.ContentParagraph contentFqdn, contentItems, contentReasons;
        defaultJSDService.createTicket(request);

        verify(jsdApiService).createTicket(argument.capture());
        JsdApiIssueRequest jsdApiIssueRequest = argument.getValue();

        contentFqdn = (JsdApiIssueRequest.ContentParagraph) jsdApiIssueRequest.fields.description.contentList.get(0);
        contentNodeFqdnText = (JsdApiIssueRequest.ContentNodeLabel) contentFqdn.contentList.get(0);
        contentNodeFqdnValue = (JsdApiIssueRequest.ContentNodeValue) contentFqdn.contentList.get(1);
        contentMark = (JsdApiIssueRequest.ContentNodeMarks) contentNodeFqdnText.marks.get(0);
        contentItems = (JsdApiIssueRequest.ContentParagraph) jsdApiIssueRequest.fields.description.contentList.get(1);
        contentNodeItemsText = (JsdApiIssueRequest.ContentNodeLabel) contentItems.contentList.get(0);
        contentNodeItemsValue = (JsdApiIssueRequest.ContentNodeValue) contentItems.contentList.get(1);
        contentReasons = (JsdApiIssueRequest.ContentParagraph) jsdApiIssueRequest.fields.description.contentList.get(2);
        contentNodeReasonsText = (JsdApiIssueRequest.ContentNodeLabel) contentReasons.contentList.get(0);
        contentNodeReasonsValue = (JsdApiIssueRequest.ContentNodeValue) contentReasons.contentList.get(1);


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
}
