package com.godaddy.vps4.orchestration.hfs.sysadmin;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class SetPassword implements Command<SetPassword.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SetPassword.class);

    public static class Request {
        public long vmId;
        public List<String> usernames;
        public String password;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public SetPassword(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    public Void execute(CommandContext context, Request request) {

        logger.debug("Setting passwords for users {} on vm {}", request.usernames.toString(), request.vmId);

        for(String username : request.usernames){

            SysAdminAction hfsSysAction = context.execute("SetPassword-"+username, ctx -> sysAdminService.changePassword(request.vmId, username, request.password));

            context.execute("WaitForSet-"+username, WaitForSysAdminAction.class, hfsSysAction);
        }

        return null;
    }


}
