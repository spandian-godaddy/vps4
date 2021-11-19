package com.godaddy.vps4.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ServiceUnavailableException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.client.VmOutageApiService;

public class Vps4PanoptaMessageHandlerTest {

    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private VmOutageApiService  vmOutageApi = mock(VmOutageApiService.class);

    private UUID vmId = UUID.randomUUID();
    private String serverKey = "5kk3-ukkv-aher-ngna";
    private VmOutage outage;
    private long panoptaOutageId = -105141431;

    @Before
    public void setUp() {
        outage = new VmOutage();
        outage.panoptaOutageId = panoptaOutageId;
        outage.metrics = Collections.singleton(VmMetric.CPU);
        when(panoptaDataService.getVmId(serverKey)).thenReturn(vmId);
    }

    private JSONObject createOutageEventMessage() {
        return createTestKafkaMessage("outage event");
    }

    private JSONObject createClearEventMessage() {
        return createTestKafkaMessage("clear event");
    }

    @SuppressWarnings("unchecked")
    private JSONObject createTestKafkaMessage(String event) {
        /*
         * Sample:
         * {
         *   "event": "outage event",
         *   "severity": "critical",
         *   "fqdn": "64.202.187.96",
         *   "name": "panopta.cent7.a2",
         *   "start": "2019-12-09 21:19:51 UTC",
         *   "reasons": "CPU greater than 95% for more than 5 minutes",
         *   "duration": "339.381149",
         *   "serverKey": "5kk3-ukkv-aher-ngna",
         *   "outageId": "-105141431",
         *   "resourceId": "17487936",
         *   "itemType": "cpu_usage/usage_percentage",
         *   "items": "CPU: Usage % - Total"
         *  }
         */
        JSONObject message = new JSONObject();
        message.put("event", event);
        message.put("serverKey", serverKey);
        message.put("start", "2019-12-09 21:19:51 UTC");
        message.put("reasons", "Something bad happened");
        message.put("itemType", "cpu_usage/usage_percentage");
        message.put("outageId", panoptaOutageId);

        return message;
    }

    @SuppressWarnings("unchecked")
    private void callHandleMessage(String message) throws MessageHandlerException {
        ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(message);
        MessageHandler handler = new Vps4PanoptaMessageHandler(panoptaDataService, vmOutageApi);
        handler.handleMessage(record);
    }

    @SuppressWarnings("unchecked")
    private <K,V> void replaceJsonKeyVal(JSONObject jsonMsg, K key, V val) {
        jsonMsg.replace(key, val);
    }

    @Test
    public void handlesOutageEventMessage() throws MessageHandlerException {
        callHandleMessage(createOutageEventMessage().toJSONString());
        verify(vmOutageApi).newVmOutage(eq(vmId), eq(panoptaOutageId));
    }

    @Test
    public void handlesClearEventMessage() throws MessageHandlerException {
        callHandleMessage(createClearEventMessage().toJSONString());
        verify(vmOutageApi).clearVmOutage(vmId, panoptaOutageId);
    }

    @Test
    public void canHandleMessageWithUnknownServerKey() throws MessageHandlerException {
        JSONObject jsonMsg = createOutageEventMessage();
        replaceJsonKeyVal(jsonMsg, "serverKey", "unknown-server-key");
        callHandleMessage(jsonMsg.toJSONString());
        verify(vmOutageApi, never()).newVmOutage(eq(vmId), eq(panoptaOutageId));
    }

    @Test
    public void canHandleMessageWithUnknownEventType() throws MessageHandlerException {
        callHandleMessage(createTestKafkaMessage("other event").toJSONString());
        verify(vmOutageApi, never()).newVmOutage(eq(vmId), eq(panoptaOutageId));
    }

    @Test
    public void ignoresBadRequestException() {
        when(vmOutageApi.newVmOutage(eq(vmId), eq(panoptaOutageId))).thenThrow(new BadRequestException());
        try {
            callHandleMessage(createOutageEventMessage().toJSONString());
            fail();
        } catch (MessageHandlerException mhex) {
            // Deliberately do not retry 4xx client errors as they are unlikely to be successful on retry
            assertFalse(mhex.shouldRetry());
        }
    }

    @Test
    public void invokesRetryOnInternalServerError() {
        when(vmOutageApi.newVmOutage(eq(vmId), eq(panoptaOutageId))).thenThrow(new InternalServerErrorException());
        try {
            callHandleMessage(createOutageEventMessage().toJSONString());
            fail();
        } catch (MessageHandlerException mhex) {
            assertTrue(mhex.shouldRetry());
        }
    }

    @Test
    public void invokesRetryOnServiceUnavailableError() {
        when(vmOutageApi.newVmOutage(eq(vmId), eq(panoptaOutageId))).thenThrow(new ServiceUnavailableException());
        try {
            callHandleMessage(createOutageEventMessage().toJSONString());
            fail();
        } catch (MessageHandlerException mhex) {
            assertTrue(mhex.shouldRetry());
        }
    }

    @Test
    public void invokesRetryOnDBError() {
        RuntimeException DBError = new RuntimeException("Sql.error.oops");
        when(vmOutageApi.newVmOutage(eq(vmId), eq(panoptaOutageId))).thenThrow(DBError);
        try {
            callHandleMessage(createOutageEventMessage().toJSONString());
            fail();
        } catch (MessageHandlerException mhex) {
            assertTrue(mhex.shouldRetry());
        }
    }

}
