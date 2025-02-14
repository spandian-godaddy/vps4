package com.godaddy.vps4.orchestration.hfs.sysadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;

public class SetHostname implements Command<SetHostname.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SetHostname.class);

    public static class Request {
        public long hfsVmId;
        public String hostname;
        public String controlPanel;

        // Empty constructor required for Jackson
        public Request(){}

        public Request(long hfsVmId, String hostname, String controlPanel){
            this.hfsVmId = hfsVmId;
            this.hostname = hostname;
            this.controlPanel = controlPanel;
        }
    }

    private final SysAdminService sysAdminService;

    @Inject
    public SetHostname(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Request request){
        logger.debug("Setting hostname to {} for hfs vm {}", request.hostname, request.hfsVmId);

        SysAdminAction hfsSysAction = context.execute("SetHostname",
                ctx -> sysAdminService.changeHostname(request.hfsVmId, request.hostname, request.controlPanel),
                SysAdminAction.class);

        context.execute("WaitForSetHostname", WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
