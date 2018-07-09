package com.godaddy.vps4.orchestration.hfs.cpanel;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlicenseCpanel implements Command<Long, Void> {

    private CPanelService cpanelService;

    @Inject
    public UnlicenseCpanel(CPanelService cpanelService) {
        this.cpanelService = cpanelService;
    }

    private static final Logger logger = LoggerFactory.getLogger(UnlicenseCpanel.class);

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        if(!vmHasCpanelLicense(hfsVmId)) {
            logger.warn("No license for vm {} found.", hfsVmId);
            return null;
        }
        CPanelAction action = context.execute("Unlicense-Cpanel", ctx -> {
            return cpanelService.licenseRelease(hfsVmId);
        }, CPanelAction.class);
        context.execute(WaitForCpanelAction.class, action);
        return null;
    }
    private boolean vmHasCpanelLicense(Long hfsVmId){
        return cpanelService.getLicenseFromDb(hfsVmId).licensedIp != null;
    }
}
