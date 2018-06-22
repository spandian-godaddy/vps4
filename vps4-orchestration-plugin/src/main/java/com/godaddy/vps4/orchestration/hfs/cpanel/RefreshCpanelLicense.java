package com.godaddy.vps4.orchestration.hfs.cpanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;

public class RefreshCpanelLicense implements Command<RefreshCpanelLicense.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(RefreshCpanelLicense.class);

    public static class Request {
        public long hfsVmId;
    }

    private final CPanelService cpanelService;

    @Inject
    public RefreshCpanelLicense(CPanelService cpanelService) {
        this.cpanelService = cpanelService;
    }

    @Override
    public Void execute(CommandContext context, Request request){
        logger.debug("Refreshing license for hfs vm {}", request.hfsVmId);

        CPanelAction hfsCpanelAction = context.execute("RefreshCPanelLicense",
                ctx -> cpanelService.licenseRefresh(request.hfsVmId),
                CPanelAction.class);

        context.execute("WaitForLicenseRefresh", WaitForCpanelAction.class, hfsCpanelAction);

        return null;
    }
}
