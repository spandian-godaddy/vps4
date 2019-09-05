package com.godaddy.vps4.orchestration.monitoring;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.util.MonitoringMeta;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.nodeping.NodePingService;

@CommandMetadata(
        name = "RemoveNodePingMonitoring",
        requestType = IpAddress.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class RemoveNodePingMonitoring implements Command<IpAddress, Void> {

    private static final Logger logger = LoggerFactory.getLogger(RemoveNodePingMonitoring.class);
    private final NodePingService hfsNodePingService;
    private final MonitoringMeta monitoringMeta;

    @Inject
    public RemoveNodePingMonitoring(NodePingService nodePingService,
            MonitoringMeta monitoringMeta) {
        this.hfsNodePingService = nodePingService;
        this.monitoringMeta = monitoringMeta;
    }

    @Override
    public Void execute(CommandContext context, IpAddress address) {
        try {
            hfsNodePingService.deleteCheck(monitoringMeta.getAccountId(), address.pingCheckId);
        } catch (NotFoundException ex) {
            logger.info("Monitoring check {} was not found", address.pingCheckId, ex);
        }

        return null;
    }
}
