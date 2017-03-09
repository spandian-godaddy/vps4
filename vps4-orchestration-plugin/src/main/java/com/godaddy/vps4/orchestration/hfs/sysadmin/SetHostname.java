package com.godaddy.vps4.orchestration.hfs.sysadmin;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class SetHostname implements Command<SetHostname.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SetHostname.class);

    public static class Request {
        public long hfsVmId;
        public String hostname;
        public String controlPanel;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public SetHostname(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    public Void execute(CommandContext context, Request request){
        logger.debug("Setting hostname to {} for hfs vm {}", request.hostname, request.hfsVmId);

        SysAdminAction hfsSysAction = context.execute("SetHostname", ctx -> sysAdminService.changeHostname(request.hfsVmId, request.hostname, request.controlPanel));

        context.execute("WaitForSetHostname", WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
