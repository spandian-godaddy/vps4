package com.godaddy.vps4.orchestration.hfs.cpanel;

import javax.inject.Inject;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelService;
import gdg.hfs.orchestration.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.CommandContext;


public class WaitForCpanelAction implements Command<CPanelAction, CPanelAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForCpanelAction.class);

    final CPanelService cPanelService;

    @Inject
    public WaitForCpanelAction(CPanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public CPanelAction execute(CommandContext context, CPanelAction hfsAction) {

        while (!hfsAction.status.equals(CPanelAction.Status.COMPLETE)
                && !hfsAction.status.equals(CPanelAction.Status.FAILED)) {

            logger.debug("waiting for cPanel action to complete: {}", hfsAction);

            context.sleep(2000);

            hfsAction = cPanelService.getAction(hfsAction.actionId);
        }

        return hfsAction;
    }

}
