package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class InstallPanopta implements Command<InstallPanopta.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(InstallPanopta.class);

    public static class Request {
        public String customerKey;
        public String templates;
        public long hfsVmId;
    }

    private final SysAdminService sysAdminService;

    @Inject
    public InstallPanopta(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        logger.info("Configuring panopta for orion guid {} ", request.hfsVmId);

        SysAdminAction hfsSysAction = context.execute("InstallPanopta-" + request.hfsVmId,
                                                      ctx -> sysAdminService.installPanopta(request.hfsVmId, request.customerKey, request.templates, null),
                                                      SysAdminAction.class);
        context.execute("WaitForPanoptaInstall-" + request.hfsVmId, WaitForSysAdminAction.class, hfsSysAction);

        return null;
    }
}
