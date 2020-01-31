package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanopta;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;

public class SetupPanopta implements Command<SetupPanopta.Request, Void> {

    public static final Logger logger = LoggerFactory.getLogger(SetupPanopta.class);

    private CreditService creditService;
    private PanoptaDataService panoptaDataService;
    private Config config;

    @Inject
    public SetupPanopta(CreditService creditService, PanoptaDataService panoptaDataService, Config config) {
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
        this.config = config;
    }

    @Override
    public Void execute(CommandContext context, Request request) {

        // get panopta details from vps4 DB
        PanoptaCustomerDetails panoptaCustomerDetails = panoptaDataService.getPanoptaCustomerDetails(request.shopperId);
        PanoptaServerDetails panoptaServerDetails = panoptaDataService.getPanoptaServerDetails(request.vmId);
        if (panoptaServerDetails != null && panoptaCustomerDetails != null
                && StringUtils.isNotBlank(panoptaServerDetails.getServerKey())) {
            // this is a rebuild vm - so install panopta on rebuild.
            logger.info("Rebuilding server with panopta with serverKey {} and customerKey {}",
                        panoptaServerDetails.getServerKey(), panoptaCustomerDetails.getCustomerKey());
            installPanoptaOnRebuild(panoptaServerDetails, panoptaCustomerDetails, request, context);
        } else {
            // check if customer exists in panopta, if not, create customer in panopta and install panopta on new vm
            logger.info("Checking if customer exists in Panopta for shopper {} vm id {}  before provisioning server...",
                        request.shopperId, request.vmId);
            panoptaCustomerDetails = createNewCustomerInPanopta(request, context);
            installPanoptaOnProvision(panoptaCustomerDetails, request, context);
        }

        return null;
    }

    private void installPanoptaOnRebuild(PanoptaServerDetails panoptaServerDetails,
                                         PanoptaCustomerDetails panoptaCustomerDetails, Request request,
                                         CommandContext context) {
        installPanoptaOnVm(panoptaCustomerDetails.getCustomerKey(), panoptaServerDetails.getServerKey(), request,
                           context);
    }

    private void installPanoptaOnProvision(PanoptaCustomerDetails panoptaCustomerDetails, Request request,
                                           CommandContext context) {
        installPanoptaOnVm(panoptaCustomerDetails.getCustomerKey(), null, request, context);
        updateVps4DbWithPanoptaServerInfo(context, request);
    }

    private PanoptaCustomerDetails createNewCustomerInPanopta(Request request, CommandContext context) {
        // create panopta customer if one does not already exist
        PanoptaCustomer panoptaCustomer = getCustomerFromPanopta(context, request);
        if (panoptaCustomer == null) {
            panoptaCustomer = createCustomerInPanopta(context, request);
        }
        savePanoptaCustomerInVps4Db(request, panoptaCustomer);
        return panoptaDataService.getPanoptaCustomerDetails(request.shopperId);
    }

    private PanoptaCustomer getCustomerFromPanopta(CommandContext context, Request request) {
        return context.execute(GetPanoptaCustomer.class, request.shopperId);
    }

    private PanoptaCustomer createCustomerInPanopta(CommandContext context, Request request) {
        CreatePanoptaCustomer.Request createCustomerRequest = new CreatePanoptaCustomer.Request();
        createCustomerRequest.vmId = request.vmId;
        createCustomerRequest.shopperId = request.shopperId;
        CreatePanoptaCustomer.Response createCustomerResponse =
                context.execute(CreatePanoptaCustomer.class, createCustomerRequest);
        return createCustomerResponse.getPanoptaCustomer();
    }

    private void savePanoptaCustomerInVps4Db(Request request, PanoptaCustomer panoptaCustomer) {
        // save panopta customer details in vps4 db
        panoptaDataService.createPanoptaCustomer(request.shopperId, panoptaCustomer.getCustomerKey());
    }

    private void updateVps4DbWithPanoptaServerInfo(CommandContext context, Request request) {
        // get the server key from HFS
        String serverKey = getServerKeyFromHfs(context, request);
        // get server information from Panopta using the server key
        PanoptaServer panoptaServer = getPanoptaServerByServerkey(context, request, serverKey);
        // save server information in VPS4 Database
        panoptaDataService.createPanoptaServer(request.vmId, request.shopperId, panoptaServer);
    }

    private String getServerKeyFromHfs(CommandContext context, Request request) {
        SysAdminAction sysAdminAction =
                context.execute(GetPanoptaServerKeyFromHfs.class, request.hfsVmId);
        String panoptaServerKey = sysAdminAction.resultSet;
        logger.info("Panopta Server Key: " + panoptaServerKey);
        return panoptaServerKey;
    }

    private PanoptaServer getPanoptaServerByServerkey(CommandContext context, Request request, String serverKey) {
        WaitForPanoptaInstall.Request getPanoptaServerRequest = new WaitForPanoptaInstall.Request();
        getPanoptaServerRequest.shopperId = request.shopperId;
        getPanoptaServerRequest.serverKey = serverKey;
        getPanoptaServerRequest.vmId = request.vmId;
        return context.execute(WaitForPanoptaInstall.class, getPanoptaServerRequest);
    }

    private void installPanoptaOnVm(String customerKey, String serverKey, SetupPanopta.Request request,
                                    CommandContext context) {

        // make HFS calls to install panopta on the vm
        InstallPanopta.Request panoptaRequest = new InstallPanopta.Request();
        panoptaRequest.hfsVmId = request.hfsVmId;
        panoptaRequest.customerKey = customerKey;
        panoptaRequest.serverKey = serverKey;
        panoptaRequest.templates = setPanoptaTemplates(request);
        context.execute(InstallPanopta.class, panoptaRequest);
    }

    private String setPanoptaTemplates(SetupPanopta.Request request) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(request.orionGuid);
        String templateType = (credit.isManaged()) ? "managed" : credit.hasMonitoring() ? "addon" : "base";

        String serverTemplate = config.get("panopta.api.templates." + templateType +
                "." + credit.getOperatingSystem().toLowerCase());
        String dcAlertTemplate = config.get("panopta.api.templates.webhook");
        return String.join(",", serverTemplate, dcAlertTemplate);
    }

    public static class Request {
        public UUID vmId;
        public UUID orionGuid;
        public long hfsVmId;
        public String shopperId;
    }
}
