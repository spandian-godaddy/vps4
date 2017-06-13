package com.godaddy.vps4.orchestration.hfs.cpanel;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;

public class WaitForCpanelAction implements Command<CPanelAction, Void> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForCpanelAction.class);

    final CPanelService cPanelService;

    @Inject
    public WaitForCpanelAction(CPanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public Void execute(CommandContext context, CPanelAction hfsAction) {

        while (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)
                && !hfsAction.status.equals(CPanelAction.Status.FAILED)) {

            logger.info("waiting on config image: {}", hfsAction);

            context.sleep(2000);

            hfsAction = cPanelService.getAction(hfsAction.actionId);
        }

        return null;
    }

}
