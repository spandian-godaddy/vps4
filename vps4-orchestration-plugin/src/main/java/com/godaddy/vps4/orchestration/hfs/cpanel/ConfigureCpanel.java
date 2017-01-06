package com.godaddy.vps4.orchestration.hfs.cpanel;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;

public class ConfigureCpanel implements Command<ConfigureCpanel.ConfigureCpanelRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureCpanel.class);

    final CPanelService cPanelService;

    @Inject
    public ConfigureCpanel(CPanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public Void execute(CommandContext context, ConfigureCpanelRequest action) {
        logger.info("sending HFS request to config cpanel image for vmId {}", action.vmId);

        CPanelAction hfsAction = context.execute("RequestFromHFS", ctx -> {
            return cPanelService.imageConfig(action.vmId, action.publicIp);
        });

        context.execute(WaitForCpanelAction.class, hfsAction);

//        if (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)) {
//            action.status = ActionStatus.ERROR;
//            logger.warn("failed to config image {}", hfsAction);
//            throw new Vps4Exception("CPANEL_IMAGE_CONFIG_FAILED", "CPanel image config failed");
//        }

//        action.status = ActionStatus.COMPLETE;
//        logger.info("config image complete: {}", hfsAction);

        return null;
    }

    public static class ConfigureCpanelRequest {
        public long vmId;
        public String publicIp;
    }
}
