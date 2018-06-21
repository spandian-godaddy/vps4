package com.godaddy.vps4.orchestration.sysadmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class ConfigureMailRelay implements Command<ConfigureMailRelayRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureMailRelay.class);

    final SysAdminService sysAdminService;

    @Inject
    public ConfigureMailRelay(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, ConfigureMailRelayRequest action) {
        logger.info("sending HFS request to configMta for to vmId {}", action.vmId);

        SysAdminAction hfsAction = sysAdminService.configureMTA(action.vmId, action.controlPanel);
        context.execute(WaitForSysAdminAction.class, hfsAction);

        return null;
    }

    public static class ConfigureMailRelayRequest {

        public long vmId;
        public String controlPanel;

        public ConfigureMailRelayRequest(){}

        public ConfigureMailRelayRequest(long vmId, String controlPanel) {
            this.vmId = vmId;
            this.controlPanel = controlPanel;
        }

    }
}
