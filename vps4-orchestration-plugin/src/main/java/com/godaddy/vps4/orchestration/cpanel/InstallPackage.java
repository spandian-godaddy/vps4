package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelBuild;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InstallPackage implements Command<InstallPackage.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(InstallPackage.class);

    private final Vps4CpanelService cPanelService;

    private CommandContext context;
    private Request request;

    @Inject
    public InstallPackage(Vps4CpanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        this.context = context;
        this.request = request;
        CpanelBuild build = installPackage();
        waitForPackageInstall(build.buildNumber);
        return null;
    }


    private CpanelBuild installPackage() {
        logger.info("Calling CPanel to install rpm package {} for hfs vm {}.", request.packageName, request.hfsVmId);
        try {
            return cPanelService.installRpmPackage(request.hfsVmId, request.packageName);
        } catch (CpanelTimeoutException | CpanelAccessDeniedException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForPackageInstall(long buildNumber) {
        WaitForPackageInstall.Request request = new WaitForPackageInstall.Request();
        request.buildNumber = buildNumber;
        request.hfsVmId = this.request.hfsVmId;
        context.execute(WaitForPackageInstall.class, request);
    }

    public static class Request {
        public long hfsVmId;
        public String packageName;
    }
}
