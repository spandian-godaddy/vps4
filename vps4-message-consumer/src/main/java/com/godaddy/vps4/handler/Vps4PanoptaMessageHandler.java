package com.godaddy.vps4.handler;

import static com.godaddy.vps4.handler.util.Utils.isDBError;
import static com.godaddy.vps4.handler.util.Utils.isVps4ApiDown;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.web.monitoring.VmOutageRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.web.client.VmOutageApiService;

public class Vps4PanoptaMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4PanoptaMessageHandler.class);

    private final PanoptaDataService panoptaDataService;
    private final VmOutageApiService vmOutageApi;

    // Event type strings are defined by Panopta
    private final String EVENT_OUTAGE = "outage event";
    private final String EVENT_CLEAR = "clear event";

    @Inject
    public Vps4PanoptaMessageHandler(PanoptaDataService panoptaDataService, VmOutageApiService vmOutageApiService) {
        this.panoptaDataService = panoptaDataService;
        this.vmOutageApi = vmOutageApiService;
    }

    @Override
    public void handleMessage(ConsumerRecord<String, String> message) throws MessageHandlerException {
        logger.info("Received panopta webhook alert : {} ", message.value());

        Vps4PanoptaMessage msg = new Vps4PanoptaMessage(message);
        UUID vmId = lookupVmId(msg.serverKey);
        if (vmId == null) {
            logger.warn("Unknown server key found in panopta webhook alert: {}, No corresponding VM found in DB", msg.serverKey);
            return;
        }

        try {
            switch (msg.event) {
                case EVENT_OUTAGE :
                    reportNewVmMetricOutages(vmId, msg);
                    break;
                case EVENT_CLEAR :
                    clearVmMetricOutages(vmId, msg);
                    break;
                default :
                    logger.warn("Unknown event found in panopta webhook alert : {}", msg.event);
            }
        } catch (Exception ex) {
            // retry the message processing for panopta if DB or Vps4API is down
            // specifically not retrying messages for 4xx client errors like BadRequest, NotFound
            // - as they would likely repeatedly fail and the consumer offset would never progress.
            boolean shouldRetry = isDBError(ex) || isVps4ApiDown(ex);
            throw new MessageHandlerException(shouldRetry, ex);
        }
    }

    private UUID lookupVmId(String serverKey) {
        return panoptaDataService.getVmId(serverKey);
    }

    private void reportNewVmMetricOutages(UUID vmId, Vps4PanoptaMessage msg) {
        long panoptaOutageId = Long.parseLong(msg.outageId);
        logger.info("Reporting new outage {} on VM {} from panopta webhook alert", msg.outageId, vmId);
        vmOutageApi.newVmOutage(vmId, panoptaOutageId);
    }

    private void clearVmMetricOutages(UUID vmId, Vps4PanoptaMessage msg) {
        long panoptaOutageId = Long.parseLong(msg.outageId);
        VmOutageRequest request = new VmOutageRequest(formatTimestamp(msg.start));
        logger.info("Clearing outage {} on VM {} from panopta webhook alert", msg.outageId, vmId);
        vmOutageApi.clearVmOutage(vmId, panoptaOutageId, request);
    }

    private String formatTimestamp(String timestamp) {
        String pattern = "yyyy-MM-dd HH:mm:ss z";
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .toFormatter()
                .withZone(ZoneId.of("UTC"));
        return formatter.parse(timestamp, Instant::from).toString();
    }
}
