package com.godaddy.vps4.orchestration.hfs.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.nodeping.NodePingAction;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class WaitForPingCheckAction implements Command<NodePingAction, NodePingAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForPingCheckAction.class);

    final NodePingService nodePingService;

    @Inject
    public WaitForPingCheckAction(NodePingService nodePingService) {
        this.nodePingService = nodePingService;
    }

    @Override
    public NodePingAction execute(CommandContext context, NodePingAction hfsAction) {
        while (!hfsAction.status.equals(NodePingAction.Status.COMPLETE)
                && !hfsAction.status.equals(NodePingAction.Status.FAILED)) {

            logger.info("waiting for nodeping action: {}", hfsAction);
            
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e){
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = nodePingService.getAction(hfsAction.actionId);
        }

        if (hfsAction.status.equals(NodePingAction.Status.COMPLETE)) {
            logger.info("NodePing action complete, hfsAction: {}", hfsAction);
        }
        else {
            throw new RuntimeException(String.format("NodePing action failed: %s", hfsAction));
        }

        return hfsAction;
    }

}
