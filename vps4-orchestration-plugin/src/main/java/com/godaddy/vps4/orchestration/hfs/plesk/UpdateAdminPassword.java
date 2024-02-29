package com.godaddy.vps4.orchestration.hfs.plesk;

import javax.inject.Inject;

import com.godaddy.hfs.plesk.PleskAdminPassRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Cryptography;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskService;

public class UpdateAdminPassword implements Command<UpdateAdminPassword.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurePlesk.class);
    private final Cryptography cryptography;
    final PleskService pleskService;

    @Inject
    public UpdateAdminPassword(PleskService pleskService, Cryptography cryptography) {
        this.pleskService = pleskService;
        this.cryptography = cryptography;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("sending HFS request to update Plesk admin password for vmId {}", request.vmId);

        String password = cryptography.decrypt(request.encryptedPassword);
        PleskAction hfsAction = context.execute("adminPassUpdateHFS",
                ctx -> pleskService.adminPassUpdate(new PleskAdminPassRequest(request.vmId, password)), PleskAction.class);

        context.execute(WaitForPleskAction.class, hfsAction);

        logger.info("Completed Plesk update admin password vm action {} ", hfsAction);
        return null;
    }

    public static class Request {
        public long vmId;
        public byte[] encryptedPassword;
    }

}
