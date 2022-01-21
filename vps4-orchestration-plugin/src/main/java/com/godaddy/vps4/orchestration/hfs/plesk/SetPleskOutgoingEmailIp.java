package com.godaddy.vps4.orchestration.hfs.plesk;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class SetPleskOutgoingEmailIp implements Command<SetPleskOutgoingEmailIp.SetPleskOutgoingEmailIpRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SetPleskOutgoingEmailIp.class);

    final PleskService pleskService;

    @Inject
    public SetPleskOutgoingEmailIp(PleskService pleskService) {
        this.pleskService = pleskService;
    }

    @Override
    public Void execute(CommandContext context, SetPleskOutgoingEmailIpRequest request) {
        logger.info("sending HFS request to set Plesk outgoing email IP for vmId {}", request.hfsVmId);
        PleskAction hfsAction = context.execute("SetPleskOutgoingEmail", ctx -> {
                return pleskService.setOutgoingEMailIP(request.hfsVmId, request.ipAddress);
        }, PleskAction.class);

       context.execute(WaitForPleskAction.class, hfsAction);

       logger.info("Completed setting Plesk outgoing email IP vm action {} ", hfsAction);
       return null;
    }

    public static class SetPleskOutgoingEmailIpRequest {
        public long hfsVmId;
        public String ipAddress;

        // Empty constructor required for Jackson
        public SetPleskOutgoingEmailIpRequest() {}

        public SetPleskOutgoingEmailIpRequest(long vmId, String ipAddress) {
            this.hfsVmId = vmId;
            this.ipAddress = ipAddress;
        }
    }

}


