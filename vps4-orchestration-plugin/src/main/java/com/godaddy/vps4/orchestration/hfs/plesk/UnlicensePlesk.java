package com.godaddy.vps4.orchestration.hfs.plesk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class UnlicensePlesk implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(UnlicensePlesk.class);

    private PleskService pleskService;

    @Inject
    public UnlicensePlesk(PleskService pleskService) {
        this.pleskService = pleskService;
    }

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        try {

            PleskAction action = context.execute("UnlicensePleskHfs",
                    ctx -> pleskService.licenseRelease(hfsVmId), PleskAction.class);
            context.execute(WaitForPleskAction.class, action);

        } catch (RuntimeException e) {
            // This logic can be revisited when HFS provides a way to lookup Plesk licenses
            if(e.getMessage().contains("VM does not have a resource ID")) {
                logger.warn("No resource ID found for HFS VM {}, ignore unlicensing error", hfsVmId);
            } else if(e.getMessage().contains("Failed to find license for VM")) {
                logger.warn("No license found for HFS VM {}, ignore unlicensing error", hfsVmId);
            } else {
                throw e;
            }
        }
        return null;
    }
}
