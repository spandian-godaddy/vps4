package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Cryptography;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.AddUserRequestBody;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;

public class AddUser implements Command<AddUser.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(AddUser.class);
    private final Cryptography cryptography;

    public static class Request {
        public long hfsVmId;
        public String username;
        public byte[] encryptedPassword;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public AddUser(SysAdminService sysAdminService, Cryptography cryptography) {
        this.sysAdminService = sysAdminService;
        this.cryptography = cryptography;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("Calling HFS to add user {} to vm {}", request.username, request.hfsVmId);

        AddUserRequestBody body = new AddUserRequestBody();
        body.serverId = request.hfsVmId;
        body.username = request.username;
        body.password = cryptography.decrypt(request.encryptedPassword);

        SysAdminAction hfsSysAdminAction = context.execute("AddUser-" + request.username,
                ctx -> sysAdminService.addUser(0, null, null, body),
                SysAdminAction.class);
        context.execute("WaitForAdd-" + request.username, WaitForSysAdminAction.class, hfsSysAdminAction);

        return null;
    }
}
