package com.godaddy.vps4.orchestration.hfs.mailrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.mailrelay.MailRelayUpdate;

public class SetMailRelayQuota implements Command<SetMailRelayQuota.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SetMailRelayQuota.class);

    private final MailRelayService mailRelayService;

    @Inject
    public SetMailRelayQuota(MailRelayService mailRelayService) {
        this.mailRelayService = mailRelayService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        MailRelayUpdate mailRelayUpdate = new MailRelayUpdate();
        mailRelayUpdate.quota = request.mailRelayQuota;
        mailRelayUpdate.relays = request.previousRelays;
        logger.info("Updating mail relay quota to {} for ip {}", mailRelayUpdate.quota, request.ipAddress);
        MailRelay relay = mailRelayService.setRelayQuota(request.ipAddress, mailRelayUpdate);
        if (relay == null || relay.quota != mailRelayUpdate.quota) {
            throw new RuntimeException(String
                    .format("Failed to set mail relay quota to %s for ip %s. ",
                            mailRelayUpdate.quota, request.ipAddress));
        }
        return null;
    }

    public static class Request {
        public String ipAddress;
        public int mailRelayQuota;
        public int previousRelays;
    }
}
