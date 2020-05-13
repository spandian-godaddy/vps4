package com.godaddy.vps4.handler;

import static com.godaddy.vps4.handler.util.Utils.isDBError;
import static com.godaddy.vps4.handler.util.Utils.isVps4ApiDown;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.vm.VmOutageService;
import com.godaddy.vps4.web.client.VmOutageApiService;
import com.godaddy.vps4.web.monitoring.VmOutageResource.VmOutageRequest;

public class Vps4PanoptaMessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4PanoptaMessageHandler.class);

    private final PanoptaDataService panoptaDataService;
    private final VmOutageApiService vmOutageApi;
    private final VmOutageService vmOutageDbService;

    // Event type strings are defined by Panopta
    private final String EVENT_OUTAGE = "outage event";
    private final String EVENT_CLEAR = "clear event";

    enum Item {
        ITEM_RAM_WIN ("perfmon.memory/percent_used"),
        ITEM_DISK_WIN("perfmon.logicaldisk/percent_used_space"),
        ITEM_CPU_WIN ("perfmon.processor/percent_processor_time"),
        ITEM_RAM ("memory/ram.percent"),
        ITEM_DISK("disk/usage.percent_used"),
        ITEM_CPU ("cpu_usage/usage_percentage"),
        ITEM_PING("icmp.ping"),
        ITEM_SSH ("tcp.ssh.port"),
        ITEM_IMAP("tcp.imap.port"),
        ITEM_POP ("tcp.pop.port"),
        ITEM_HTTP("tcp.http.port"),
        ITEM_FTP ("tcp.ftp.port"),
        ITEM_SMTP("tcp.smtp.port");

        private final String textkey;

        Item(String textkey) {
            this.textkey = textkey;
        }

        private final static Map<String, Item> map = stream(Item.values())
                .collect(toMap(item -> item.textkey, item -> item));

        public static Item get(String textkey) {
            return map.get(textkey);
        }

        public String getTextkey() {
            return textkey;
        }
    }

    @Inject
    public Vps4PanoptaMessageHandler(PanoptaDataService panoptaDataService,
            VmOutageApiService vmOutageApiService,
            VmOutageService vmOutageDbService) {
        this.panoptaDataService = panoptaDataService;
        this.vmOutageApi = vmOutageApiService;
        this.vmOutageDbService = vmOutageDbService;
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
        // msg.itemType is comma delimited list
        Set<String> itemSet = Arrays.stream(msg.itemType.split((","))).collect(Collectors.toSet());
        for (String itemType: itemSet) {
            VmMetric metric = mapItemTypeToMetric(itemType);
            if (metric == VmMetric.UNKNOWN) {
                logger.warn("Unknown metric found in panopta webhook alert: {}", msg.itemType);
                continue;
            }

            VmOutageRequest req = new VmOutageRequest();
            req.metric = metric.name();
            req.startDate = msg.start;
            req.reason = msg.reasons;
            req.panoptaOutageId = Long.parseLong(msg.outageId);
            logger.info("Reporting new outage on VM {} for metric {} from panopta webhook alert", vmId, metric.name());
            vmOutageApi.newVmOutage(vmId, req);
        }
    }

    private VmMetric mapItemTypeToMetric(String itemType) {
        Item item = Item.get(itemType);
        if (item == null)
            return VmMetric.UNKNOWN;

        switch (item) {
            case ITEM_RAM :
            case ITEM_RAM_WIN :
                return VmMetric.RAM;
            case ITEM_DISK :
            case ITEM_DISK_WIN :
                return VmMetric.DISK;
            case ITEM_CPU :
            case ITEM_CPU_WIN :
                return VmMetric.CPU;
            case ITEM_PING :
                return VmMetric.PING;
            case ITEM_SSH :
                return VmMetric.SSH;
            case ITEM_IMAP :
                return VmMetric.IMAP;
            case ITEM_POP :
                return VmMetric.POP3;
            case ITEM_HTTP :
                return VmMetric.HTTP;
            case ITEM_FTP :
                return VmMetric.FTP;
            case ITEM_SMTP :
                return VmMetric.SMTP;
            default :
                return VmMetric.UNKNOWN;
        }
    }

    private void clearVmMetricOutages(UUID vmId, Vps4PanoptaMessage msg) {
        long panoptaOutageId = Long.parseLong(msg.outageId);
        List<VmOutage> outageList = lookupVmOutageList(panoptaOutageId);
        if (outageList.isEmpty()) {
            logger.warn("Unknown outage ID found in panopta webhook alert: {}, No corresponding VM outages found in DB", msg.outageId);
            return;
        }

        for (VmOutage outage: outageList) {
            logger.info("Clearing outage on VM {} for metric {} from panopta webhook alert", vmId, outage.metric.name());
            vmOutageApi.clearVmOutage(vmId, outage.outageId, msg.start);
        }
    }

    private List<VmOutage> lookupVmOutageList(long panoptaOutageId) {
        return vmOutageDbService.getVmOutageList(panoptaOutageId);
    }
}
