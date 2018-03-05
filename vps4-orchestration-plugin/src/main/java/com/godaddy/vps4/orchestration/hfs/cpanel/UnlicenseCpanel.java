package com.godaddy.vps4.orchestration.hfs.cpanel;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;

public class UnlicenseCpanel implements Command<Long, Void> {

    private CPanelService cpanelService;

    @Inject
    public UnlicenseCpanel(CPanelService cpanelService) {
        this.cpanelService = cpanelService;
    }

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        CPanelAction action = context.execute("Unlicense-Cpanel", ctx -> {
            return cpanelService.licenseRelease(hfsVmId);
        }, CPanelAction.class);
        context.execute(WaitForCpanelAction.class, action);
        return null;
    }
}
