package com.godaddy.vps4.plesk;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.PollerTimedOutException;
import com.godaddy.vps4.util.Vps4Poller;

import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskAction.Status;
import gdg.hfs.vhfs.plesk.PleskService;

public class Vps4PleskActionPoller implements Vps4Poller<PleskAction, Integer, String> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4PleskActionPoller.class);
    
    private PleskService pleskService;
    
    @Inject
    public Vps4PleskActionPoller(PleskService pleskService) {
        this.pleskService = pleskService;
    }

    @Override
    public String poll(PleskAction pleskAction, Integer timeoutValue) throws PollerTimedOutException {
        
        long pleskActionId = pleskAction.actionId;
        Instant expiration = Instant.now().plus(timeoutValue, ChronoUnit.MILLIS);

        logger.info("Started polling for action id: {} , polling till expiration: {} ", pleskActionId, expiration.toString());

        while (Instant.now().isBefore(expiration) && 
                (pleskAction.status == Status.NEW || pleskAction.status == Status.IN_PROGRESS)) {

            pleskAction = pleskService.getAction(pleskActionId);

            if (pleskAction.status == Status.COMPLETE) {
                return pleskAction.responsePayload;
            }

            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.info("Interrupted while sleeping ");
            }
        }

        if (pleskAction.status != Status.COMPLETE) {
            // any other issue is bubbled as a general timeout exception
            String message = String.format("Timed out retrying an operation on VM {} ", pleskAction.vmId);
            logger.warn(message);
            throw new PollerTimedOutException(message);
        }
        return null;
    }
}
