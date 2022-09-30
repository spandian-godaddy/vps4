package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

@CommandMetadata(
        name = "Vps4InstallCPanelPackage",
        requestType = Vps4InstallCPanelPackage.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4InstallCPanelPackage extends ActionCommand<Vps4InstallCPanelPackage.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4InstallCPanelPackage.class);
    private CommandContext context;
    private Vps4CpanelService cPanelService;

    @Inject
    public Vps4InstallCPanelPackage(ActionService actionService, Vps4CpanelService cPanelService) {
        super(actionService);
        this.cPanelService = cPanelService;
    }

    public static class Request extends Vps4ActionRequest {
        public Long hfsVmId;
        public String packageName;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        this.context = context;

        logger.info("Installing package {} for vmId: {}", request.packageName, request.vmId);

        installPackage(request);

        logger.info("Rpm package update done. Confirming package {} is installed for vmId: {}", request.packageName, request.vmId);

        if (!confirmPackageIsInstalled(request)) {
            throw new RuntimeException("Rpm Package Install failed for package "+ request.packageName + " and vmId "+ request.vmId);
        }
        return null;
    }

    private void installPackage(Request request) {
        InstallPackage.Request req = new InstallPackage.Request();
        req.hfsVmId = request.hfsVmId;
        req.packageName = request.packageName;
        context.execute(InstallPackage.class, req);
    }

    private boolean confirmPackageIsInstalled(Request request) {
        List<String> installedPackageList;
        installedPackageList = context.execute("ListInstalledRpmPackages",
                    ctx -> {
                        try {
                            return cPanelService.listInstalledRpmPackages(request.hfsVmId);
                        } catch (CpanelAccessDeniedException | CpanelTimeoutException e) {
                            throw new RuntimeException("Could not retrieve installed rpm packages for vm : " + request.vmId, e);
                        }
                    }, List.class);
        return installedPackageList != null && installedPackageList.contains(request.packageName);
    }

}
