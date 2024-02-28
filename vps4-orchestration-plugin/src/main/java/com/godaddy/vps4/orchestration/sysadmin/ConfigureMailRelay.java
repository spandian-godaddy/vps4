package com.godaddy.vps4.orchestration.sysadmin;

import com.godaddy.vps4.vm.Image.ControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.orchestration.sysadmin.ConfigureMailRelay.ConfigureMailRelayRequest;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.sysadmin.SysAdminService;
import com.godaddy.hfs.sysadmin.SysAdminAction;

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

        SysAdminAction hfsAction = context.execute("ConfigureMta",
                ctx -> sysAdminService.configureMTA(action.vmId, action.controlPanel),
                SysAdminAction.class);

        context.execute(WaitForSysAdminAction.class, hfsAction);

        return null;
    }

    public static class ConfigureMailRelayRequest {

        public long vmId;
        public String controlPanel;

        // Empty constructor required for Jackson
        public ConfigureMailRelayRequest(){}

        public ConfigureMailRelayRequest(long vmId, ControlPanel controlPanel) {
            String panel = controlPanel.equals(ControlPanel.MYH) ? null : controlPanel.name().toLowerCase();
            this.vmId = vmId;
            this.controlPanel = panel;
        }

    }
}
