package com.godaddy.vps4.web.cpanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.web.Action.ActionStatus;

import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;

public class ImageConfigWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ImageConfigWorker.class);

    final CPanelService cPanelService;

    final ImageConfigAction action;

    public ImageConfigWorker(ImageConfigAction action, CPanelService cPanelService) {
        this.action = action;
        this.cPanelService = cPanelService;
    }

    @Override
    public void run() {
        logger.info("sending HFS request to config cpanel image for vmId {}", action.vmId);

        CPanelAction hfsAction = cPanelService.imageConfig(action.vmId, action.publicIp);

        while (!hfsAction.status.equals(CPanelAction.Status.COMPLETE) && !hfsAction.status.equals(CPanelAction.Status.FAILED)) {
            logger.info("waiting on config image: {}", hfsAction);

            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = cPanelService.getAction(hfsAction.actionId);
        }

        if (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)) {
            action.status = ActionStatus.ERROR;
            logger.warn("failed to config image {}", hfsAction);
            throw new Vps4Exception("CPANEL_IMAGE_CONFIG_FAILED", "CPanel image config failed");
        }

        action.status = ActionStatus.COMPLETE;
        logger.info("config image complete: {}", hfsAction);
    }

}
