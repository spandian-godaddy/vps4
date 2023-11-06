package com.godaddy.vps4.orchestration.hfs.plesk;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskService;

public class WaitForPleskAction implements Command<PleskAction, Void> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForPleskAction.class);

    final PleskService pleskService;

    @Inject
    public WaitForPleskAction(PleskService pleskService) {
        this.pleskService = pleskService;
    }

    @Override
    public Void execute(CommandContext context, PleskAction hfsAction) {

        while (hfsAction.status != PleskAction.Status.COMPLETE
                && hfsAction.status != PleskAction.Status.FAILED) {

            logger.debug("waiting on plesk config image: {}", hfsAction);

            context.sleep(2000);

            hfsAction = pleskService.getAction(hfsAction.actionId);
        }

        if(hfsAction.status == PleskAction.Status.COMPLETE) {
            logger.info("Vm Action completed. hfsAction: {} ", hfsAction );
        } else {
            throw new RuntimeException(String.format(" Failed action: %s", hfsAction));
        }

        return null;
    }

}
