package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class AddUser implements Command<AddUser.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(AddUser.class);

    public static class Request {
        public long hfsVmId;
        public String username;
        public String password;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public AddUser(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("Calling HFS to add user {} to vm {}", request.username, request.hfsVmId);

        SysAdminAction hfsSysAdminAction = context.execute("AddUser-" + request.username,
                ctx -> sysAdminService.addUser(request.hfsVmId, request.username, request.password),
                SysAdminAction.class);
        context.execute("WaitForAdd-" + request.username, WaitForSysAdminAction.class, hfsSysAdminAction);

        return null;
    }
}
