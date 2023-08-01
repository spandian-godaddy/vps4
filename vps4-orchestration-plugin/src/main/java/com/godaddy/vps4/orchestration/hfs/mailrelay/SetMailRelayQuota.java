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
        setQuotaUpdateValue(request, mailRelayUpdate);
        mailRelayUpdate.relays = request.relays;

        logger.info("Updating mail relay quota to {}, relays to {} for ip {}",
                mailRelayUpdate.quota, mailRelayUpdate.relays, request.ipAddress);
        MailRelay relay = mailRelayService.setRelayQuota(request.ipAddress, mailRelayUpdate);
        handleUpdateFailure(request, mailRelayUpdate, relay);
        return null;
    }

    private static void handleUpdateFailure(Request request, MailRelayUpdate mailRelayUpdate, MailRelay relay) {
        if (relay == null || relay.quota != mailRelayUpdate.quota || relay.relays != mailRelayUpdate.relays) {
            throw new RuntimeException(String.format("mail relay update failed for ip %s. ", request.ipAddress));
        }
    }

    private void setQuotaUpdateValue(Request request, MailRelayUpdate mailRelayUpdate) {
        if(request.quota == 0 && !request.isAdditionalIp) {
            MailRelay mailRelay = mailRelayService.getMailRelay(request.ipAddress);
            mailRelayUpdate.quota = mailRelay.quota;
        }
        else {
            mailRelayUpdate.quota = request.quota;
        }
    }

    public static class Request {
        public String ipAddress;
        public boolean isAdditionalIp = false;
        public int quota;
        public int relays;
    }
}
