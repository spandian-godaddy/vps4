package com.godaddy.vps4.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.handler.Vps4PanoptaMessageHandler.Item;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.vm.VmOutageService;
import com.godaddy.vps4.web.client.VmOutageApiService;
import com.godaddy.vps4.web.monitoring.VmOutageResource.VmOutageRequest;

public class Vps4PanoptaMessageHandlerTest {

    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private VmOutageApiService  vmOutageApi = mock(VmOutageApiService.class);
    private VmOutageService vmOutageDbService = mock(VmOutageService.class);

    private UUID vmId = UUID.randomUUID();
    private String serverKey = "5kk3-ukkv-aher-ngna";
    private long panoptaOutageId = -105141431;
    private int outageId = 23;

    @Before
    public void setUp() {
        when(panoptaDataService.getVmId(serverKey)).thenReturn(vmId);
        when(vmOutageDbService.getVmOutageId(panoptaOutageId)).thenReturn(outageId);
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
        MessageHandler handler = new Vps4PanoptaMessageHandler(panoptaDataService, vmOutageApi, vmOutageDbService);
        handler.handleMessage(record);
    }

    @SuppressWarnings("unchecked")
    private <K,V> void replaceJsonKeyVal(JSONObject jsonMsg, K key, V val) {
        jsonMsg.replace(key, val);
    }

    @Test
    public void handlesOutageEventMessage() throws MessageHandlerException {
        ArgumentCaptor<VmOutageRequest> argument = ArgumentCaptor.forClass(VmOutageRequest.class);
        callHandleMessage(createOutageEventMessage().toJSONString());
        verify(vmOutageApi).newVmOutage(eq(vmId), argument.capture());
        VmOutageRequest req = argument.getValue();
        assertEquals("CPU", req.metric);
        assertEquals("2019-12-09 21:19:51 UTC", req.startDate);
        assertEquals("Something bad happened", req.reason);
        assertEquals(panoptaOutageId, req.panoptaOutageId);
    }

    @Test
    public void handlesClearEventMessage() throws MessageHandlerException {
        callHandleMessage(createClearEventMessage().toJSONString());
        verify(vmOutageApi).clearVmOutage(vmId, outageId, "2019-12-09 21:19:51 UTC");
    }

    @Test
    public void canHandleMessageWithUnknownServerKey() throws MessageHandlerException {
        JSONObject jsonMsg = createOutageEventMessage();
        replaceJsonKeyVal(jsonMsg, "serverKey", "unknown-server-key");
        callHandleMessage(jsonMsg.toJSONString());
        verify(vmOutageApi, never()).newVmOutage(eq(vmId), any(VmOutageRequest.class));
    }

    @Test
    public void canHandleMessageWithUnknownEventType() throws MessageHandlerException {
        callHandleMessage(createTestKafkaMessage("other event").toJSONString());
        verify(vmOutageApi, never()).newVmOutage(eq(vmId), any(VmOutageRequest.class));
    }

    @Test
    public void canHandleMessageWithUnknownMetric() throws MessageHandlerException {
        JSONObject jsonMsg = createOutageEventMessage();
        replaceJsonKeyVal(jsonMsg, "itemType", "unknown/itemType");
        callHandleMessage(jsonMsg.toJSONString());
        verify(vmOutageApi, never()).newVmOutage(eq(vmId), any(VmOutageRequest.class));
    }

    @Test
    public void canHandleMessageWithUnknownOutageId() throws MessageHandlerException {
        when(vmOutageDbService.getVmOutageId(panoptaOutageId)).thenReturn(null);
        callHandleMessage(createClearEventMessage().toJSONString());
        verify(vmOutageApi, never()).clearVmOutage(eq(vmId), anyInt(), anyString());
    }

    @Test
    public void canHandleMessagesOfAllKnownMetrics() throws MessageHandlerException {
        JSONObject jsonMsg = createOutageEventMessage();
        for (Item item : Item.values()) {
            replaceJsonKeyVal(jsonMsg, "itemType", item.getTextkey());
            callHandleMessage(jsonMsg.toJSONString());
            verify(vmOutageApi, atLeast(1)).newVmOutage(eq(vmId), any(VmOutageRequest.class));
        }
    }

}
