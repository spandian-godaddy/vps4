package com.godaddy.vps4.orchestration.hfs.sysadmin;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Cryptography;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.ChangePasswordRequestBody;

public class SetPassword implements Command<SetPassword.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SetPassword.class);

    public static class Request {
        public long hfsVmId;
        public List<String> usernames;
        public byte[] encryptedPassword;
        public String controlPanel;
    }

    private final SysAdminService sysAdminService;
    private final Cryptography cryptography;

    @Inject
    public SetPassword(SysAdminService sysAdminService, Cryptography cryptography) {
        this.sysAdminService = sysAdminService;
        this.cryptography = cryptography;
    }

    @Override
    public Void execute(CommandContext context, Request request) {

        logger.debug("Setting passwords for users {} on vm {}", request.usernames.toString(), request.hfsVmId);
        String password = cryptography.decrypt(request.encryptedPassword);
        for(String username : request.usernames){
            ChangePasswordRequestBody body = new ChangePasswordRequestBody();
            body.username = username;
            body.password = password;
            body.controlPanel = request.controlPanel;
            body.serverId = request.hfsVmId;
            SysAdminAction hfsSysAction = context.execute("SetPassword-" + username,
                    ctx -> sysAdminService.changePassword(0, null, null, null, body),
                    SysAdminAction.class);

            context.execute("WaitForSet-"+username, WaitForSysAdminAction.class, hfsSysAction);
        }

        return null;
    }
}
