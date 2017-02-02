package com.godaddy.vps4.orchestration.hfs.smtp;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.mailrelay.MailRelayAction;
import gdg.hfs.vhfs.mailrelay.MailRelayService;

public class WaitForMailRelayAction implements Command<MailRelayAction, MailRelayAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForMailRelayAction.class);

    final MailRelayService mailRelayService;

    @Inject
    public WaitForMailRelayAction(MailRelayService mailRelayService) {
        this.mailRelayService = mailRelayService;
    }

    @Override
    public MailRelayAction execute(CommandContext context, MailRelayAction hfsAction) {
        while (!hfsAction.status.equals(MailRelayAction.Status.COMPLETE)
                && !hfsAction.status.equals(MailRelayAction.Status.ERROR)) {
            logger.info("waiting on mail relay action: {}", hfsAction);

            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                logger.warn("Interrupted while sleeping");
            }

            hfsAction = mailRelayService.getMailRelayAction(hfsAction.id, hfsAction.action_id);
        }

        if (hfsAction.status.equals(MailRelayAction.Status.COMPLETE)) {
            logger.info("Create mail relay completed. hfsAction: {} ", hfsAction);
        }
        else {
            throw new Vps4Exception("CREATE_MAIL_RELAY_FAILED", String.format("Failed action: %s", hfsAction));
        }

        return hfsAction;
    }

}
