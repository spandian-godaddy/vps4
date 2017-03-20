package com.godaddy.vps4.orchestration.sysadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMta.ConfigureMtaRequest;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class ConfigureMta implements Command<ConfigureMtaRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureMta.class);

    final SysAdminService sysAdminService;

    @Inject
    public ConfigureMta(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, ConfigureMtaRequest action) {
        logger.info("sending HFS request to configMta for to vmId {}", action.vmId);

        SysAdminAction hfsAction = context.execute("ConfigureMta", ctx -> sysAdminService.configureMTA(action.vmId, action.controlPanel));

        context.execute(WaitForSysAdminAction.class, hfsAction);

        return null;
    }

    public static class ConfigureMtaRequest {

        public long vmId;
        public String controlPanel;

        public ConfigureMtaRequest(long vmId, String controlPanel) {
            this.vmId = vmId;
            this.controlPanel = controlPanel;
        }

    }
}
