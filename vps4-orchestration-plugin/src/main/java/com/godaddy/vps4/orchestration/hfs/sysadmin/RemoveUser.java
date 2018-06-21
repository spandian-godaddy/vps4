package com.godaddy.vps4.orchestration.hfs.sysadmin;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class RemoveUser implements Command<RemoveUser.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(RemoveUser.class);

    public static class Request {
        public long hfsVmId;
        public String username;
        public UUID vmId;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public RemoveUser(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("Calling HFS to remove user {} from vm {}", request.username, request.hfsVmId);

        SysAdminAction hfsSysAdminAction = sysAdminService.removeUser(request.hfsVmId, request.username);
        context.execute("WaitForRemove-" + request.username, WaitForSysAdminAction.class, hfsSysAdminAction);

        return null;
    }
}
