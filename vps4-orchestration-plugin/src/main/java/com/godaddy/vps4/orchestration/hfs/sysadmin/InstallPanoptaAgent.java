package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class InstallPanoptaAgent implements Command<InstallPanoptaAgent.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(InstallPanoptaAgent.class);

    public static class Request {
        public String customerKey;
        public String serverKey;
        public String serverName;
        public String templates;
        public String fqdn;
        public long hfsVmId;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public InstallPanoptaAgent(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("Configuring panopta agent for hfs vm id {} ", request.hfsVmId);

        SysAdminAction hfsSysAction = context.execute("InstallPanoptaAgent",
                ctx -> sysAdminService.installPanopta(request.hfsVmId, request.customerKey, request.templates,
                                                      request.serverName, request.serverKey, request.fqdn, false),
                SysAdminAction.class);
        context.execute("WaitForPanoptaInstall-" + request.hfsVmId, WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
