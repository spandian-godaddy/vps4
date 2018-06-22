package com.godaddy.vps4.orchestration.hfs.vm;

import javax.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class RestartVm implements Command<Long, Void> {

    @Inject
    public RestartVm() {

    }

    @Override
    public Void execute(CommandContext context, Long vmId) {

        context.execute(StopVm.class, vmId);
        context.execute(StartVm.class, vmId);

        return null;
    }
}
