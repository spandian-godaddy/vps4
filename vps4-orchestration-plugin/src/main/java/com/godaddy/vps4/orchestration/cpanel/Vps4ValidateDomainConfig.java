package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class Vps4ValidateDomainConfig implements Command<Vps4ValidateDomainConfig.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4ValidateDomainConfig.class);
    private final Vps4CpanelService cPanelService;

    @Inject
    public Vps4ValidateDomainConfig(Vps4CpanelService cPanelService) { this.cPanelService = cPanelService; }

    public static class Request {
        public UUID vmId;
        public Long hfsVmId;
    }

    @Override
    public Void execute(CommandContext commandContext, Request request) {
        String allowRemoteDomainsKey = "allowremotedomains";
        String allowUnregisteredDomainsKey = "allowunregistereddomains";

        boolean allowRemoteDomains = getDomainConfig(request, allowRemoteDomainsKey);
        boolean allowUnregisteredDomains = getDomainConfig(request, allowUnregisteredDomainsKey);

        if (!allowRemoteDomains)
            updateDomainConfig(request, allowRemoteDomainsKey);

        if (!allowUnregisteredDomains)
            updateDomainConfig(request, allowUnregisteredDomainsKey);

        return null;
    }

    private boolean getDomainConfig(Request request, String key) {
        String result;

        try {
            result = cPanelService.getTweakSettings(request.hfsVmId, key);
        } catch (CpanelAccessDeniedException | CpanelTimeoutException e) {
            throw new RuntimeException("Failed to retrieve the tweak setting for " + key + " for VM: " + request.vmId, e);
        }

        return result != null && Integer.parseInt(result) == 1;
    }

    private void updateDomainConfig(Request request, String key) {
        String ENABLED = "1";

        try {
            cPanelService.setTweakSettings(request.hfsVmId, key, ENABLED);
        } catch (CpanelAccessDeniedException | CpanelTimeoutException e) {
            throw new RuntimeException("Failed to update the tweak setting for " + key + " for VM: " + request.vmId, e);
        }
    }
}
