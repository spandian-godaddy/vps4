package com.godaddy.vps4.orchestration.hfs.plesk;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

import javax.ws.rs.ClientErrorException;

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
    private long hfsVmId;

    @Inject
    public UnlicensePlesk(PleskService pleskService) {
        this.pleskService = pleskService;
    }

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        this.hfsVmId = hfsVmId;
        try {

            PleskAction action = context.execute("UnlicensePleskHfs",
                    ctx -> pleskService.licenseRelease(hfsVmId), PleskAction.class);
            context.execute(WaitForPleskAction.class, action);

        } catch (RuntimeException e) {
            // This logic can be revisited when HFS provides a way to lookup Plesk licenses
            if (shouldThrowException(getRootCause(e))) {
                throw e;
            }
        }
        return null;
    }

    private boolean shouldThrowException(Throwable t) {
        if (t instanceof ClientErrorException) {
            ClientErrorException cee = (ClientErrorException) t;
            if (cee.getResponse().readEntity(String.class).contains("VM does not have a resource ID")) {
                logger.warn("No resource ID found for HFS VM {}, ignore unlicensing error", hfsVmId);
                return false;
            }
        }

        return true;
    }

}
