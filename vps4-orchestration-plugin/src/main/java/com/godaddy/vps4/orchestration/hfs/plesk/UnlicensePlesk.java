package com.godaddy.vps4.orchestration.hfs.plesk;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class UnlicensePlesk implements Command<Long, Void> {

    private PleskService pleskService;

    @Inject
    public UnlicensePlesk(PleskService pleskService) {
        this.pleskService = pleskService;
    }

    public static class Request {
        long hfsVmId;
    }

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        PleskAction action = context.execute("Unlicense-Plesk", ctx -> {
            return pleskService.licenseRelease(hfsVmId);
        }, PleskAction.class);
        context.execute(WaitForPleskAction.class, action);
        return null;
    }
}
