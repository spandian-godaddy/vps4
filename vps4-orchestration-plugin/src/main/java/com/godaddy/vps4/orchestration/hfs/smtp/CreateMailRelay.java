package com.godaddy.vps4.orchestration.hfs.smtp;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddressValidator;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.mailrelay.MailRelayAction;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.mailrelay.MailRelayTarget;

public class CreateMailRelay implements Command<CreateMailRelay.Request, MailRelayTarget> {

    private static final Logger logger = LoggerFactory.getLogger(CreateMailRelay.class);

    public static class Request {
        public String ipAddress;

        public Request(String ipAddress) {
            this.ipAddress = ipAddress;
        }
    }

    final MailRelayService mailRelayService;

    @Inject
    public CreateMailRelay(MailRelayService mailRelayService) {
        this.mailRelayService = mailRelayService;
    }

    @Override
    public MailRelayTarget execute(CommandContext context, CreateMailRelay.Request request) {

        logger.info("sending HFS request to create mail relay for ip address {}", request.ipAddress);

        IpAddressValidator.validateIpAddress(request.ipAddress);

        MailRelayAction hfsAction = context.execute("RequestMailRelayFromHfs",
                ctx -> mailRelayService.createMailRelay(request.ipAddress));

        context.execute(WaitForMailRelayAction.class, hfsAction);

        MailRelayTarget mailRelayTarget = mailRelayService.getTargetSpec(request.ipAddress);

        logger.info("Create mail relay is complete: {}", hfsAction);
        logger.info("MailRelay: {}", mailRelayTarget);

        return mailRelayTarget;
    }
}
