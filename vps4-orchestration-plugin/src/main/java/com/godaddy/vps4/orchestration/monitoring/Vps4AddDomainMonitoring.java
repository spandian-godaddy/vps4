package com.godaddy.vps4.orchestration.monitoring;

import java.net.URL;
import java.util.UUID;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.panopta.AddAdditionalFqdnPanopta;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4AddDomainMonitoring",
        requestType=Vps4AddDomainMonitoring.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4AddDomainMonitoring extends ActionCommand<Vps4AddDomainMonitoring.Request, Void> {
    private CommandContext context;

    @Inject
    public Vps4AddDomainMonitoring(ActionService actionService) {
        super(actionService);
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4AddDomainMonitoring.Request request) {
        this.context = context;
        boolean isHttps = request.overrideProtocol == null ? isHttps(request.additionalFqdn) :
                request.overrideProtocol.equals("HTTPS");
        addAdditionalFqdn(request.vmId, request.additionalFqdn, isHttps, request.osTypeId, request.isManaged);
        return null;
    }

    private boolean isHttps(String additionalFqdn) {
        try {
            URL httpsUrl = new URL("https://" + additionalFqdn);
            HttpsURLConnection resultHttps = (HttpsURLConnection) httpsUrl.openConnection();
            resultHttps.getResponseCode();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void addAdditionalFqdn(UUID vmId, String additionalFqdn, boolean isHttps, int operatingSystemId,
                                   boolean isManaged) {
        AddAdditionalFqdnPanopta.Request addFqdnRequest = new AddAdditionalFqdnPanopta.Request();
        addFqdnRequest.vmId = vmId;
        addFqdnRequest.additionalFqdn = additionalFqdn;
        addFqdnRequest.isHttps = isHttps;
        addFqdnRequest.operatingSystemId = operatingSystemId;
        addFqdnRequest.isManaged = isManaged;
        context.execute(AddAdditionalFqdnPanopta.class, addFqdnRequest);
    }

    public static class Request implements ActionRequest {
        public UUID vmId;
        public long actionId;
        public String additionalFqdn;
        public int osTypeId;
        public boolean isManaged;
        public String overrideProtocol;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }

}