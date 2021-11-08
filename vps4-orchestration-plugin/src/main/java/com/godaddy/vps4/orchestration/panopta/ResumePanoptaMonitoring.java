package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaService;

import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "ResumePanoptaMonitoring",
        requestType = UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class ResumePanoptaMonitoring implements Command<VirtualMachine, Void> {
    private final PanoptaService panoptaService;

    @Inject
    public ResumePanoptaMonitoring(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, VirtualMachine vm) {
        panoptaService.resumeServerMonitoring(vm.vmId, vm.orionGuid);
        return null;
    }
}
