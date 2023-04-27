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

@CommandMetadata(
        name = "Vps4AddAddonDomain",
        requestType = Vps4AddAddOnDomain.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4AddAddOnDomain extends ActionCommand<Vps4AddAddOnDomain.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(Vps4AddAddOnDomain.class);
    private CommandContext context;
    private final Vps4CpanelService cPanelService;

    @Inject
    public Vps4AddAddOnDomain(ActionService actionService, Vps4CpanelService cPanelService) {
        super(actionService);
        this.cPanelService = cPanelService;
    }

    public static class Request extends Vps4ActionRequest {
        public Long hfsVmId;
        public String username;
        public String newDomain;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {
        this.context = context;

        logger.info("Adding add-on domain to cPanel account: {} with new domain: {} for vmId: {}",
                request.username, request.newDomain, request.vmId);

        validateConfig(request);
        addAddonDomain(request);
        return null;
    }

    private void validateConfig(Request request) {
        Vps4ValidateDomainConfig.Request req = new Vps4ValidateDomainConfig.Request();
        req.vmId = request.vmId;
        req.hfsVmId = request.hfsVmId;
        context.execute(Vps4ValidateDomainConfig.class, req);
    }

    private void addAddonDomain(Request request) {
        String result;
        try {
            result = cPanelService.addAddOnDomain(request.hfsVmId, request.username, request.newDomain);
        } catch (CpanelAccessDeniedException | CpanelTimeoutException e) {
            throw new RuntimeException("Could not add an add-on domain to VM: " + request.vmId, e);
        }

        if (!(result != null && result.equals("1")))
            throw new RuntimeException("The cPanel call to add an add-on domain for vmId: " + request.vmId + " has encountered an error: " + result);
    }
}
