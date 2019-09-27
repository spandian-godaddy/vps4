package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.hfs.sysadmin.InstallPanopta;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

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
        PanoptaDetail panoptaDetail = panoptaDataService.getPanoptaDetails(request.vmId);
        if (panoptaDetail != null) {
            // Since we already have the panopta details, this is likely a rebuild of vm where panopta was already
            // installed on previous vm, so we just install panopta on the new vm using the existing server_key
            installPanoptaOnVm(panoptaDetail.getCustomerKey(), panoptaDetail.getServerKey(), request, context);
        } else {
            // if panopta was never installed on the vm, this is a first time panopta install
            // create panopta customer in Panopta
            PanoptaCustomer panoptaCustomer = getPanoptaCustomer(context, request);

            // install panopta on the vm, no server key is passed in since this is a brand new install
            installPanoptaOnVm(panoptaCustomer.getCustomerKey(), null, request, context);

            // get server information from Panopta and save in VPS4 Database
            getServerDetailsAndSaveInVps4Db(context, request, panoptaCustomer);
        }

        // update credit to reflect panopta was installed
        creditService.setPanoptaInstalled(request.orionGuid, true);
        return null;
    }

    private void getServerDetailsAndSaveInVps4Db(CommandContext context, Request request,
                                                 PanoptaCustomer panoptaCustomer) {
        GetPanoptaServerDetails.Request serverDetailsRequest =
                getServerDetailsRequest(request, panoptaCustomer);
        GetPanoptaServerDetails.Response getPanoptaServerDetailsResponse =
                context.execute(GetPanoptaServerDetails.class, serverDetailsRequest);
        PanoptaServer panoptaServer = getPanoptaServerDetailsResponse.getPanoptaServer();
        logger.info("Panopta Server Details: " + panoptaServer.toString());
    }

    private PanoptaCustomer getPanoptaCustomer(CommandContext context, Request request) {
        CreatePanoptaCustomer.Request createCustomerRequest = new CreatePanoptaCustomer.Request();
        createCustomerRequest.vmId = request.vmId;
        CreatePanoptaCustomer.Response createCustomerResponse =
                context.execute(CreatePanoptaCustomer.class, createCustomerRequest);
        PanoptaCustomer panoptaCustomer = createCustomerResponse.panoptaCustomer;
        logger.info("Customer created in Panopta: " + panoptaCustomer.toString());
        return panoptaCustomer;
    }

    private GetPanoptaServerDetails.Request getServerDetailsRequest(Request request,
                                                                    PanoptaCustomer panoptaCustomer) {
        GetPanoptaServerDetails.Request getPanoptaServerDetailsRequest = new GetPanoptaServerDetails.Request();
        getPanoptaServerDetailsRequest.partnerCustomerKey = panoptaCustomer.partnerCustomerKey;
        getPanoptaServerDetailsRequest.vmId = request.vmId;
        getPanoptaServerDetailsRequest.panoptaCustomer = panoptaCustomer;
        return getPanoptaServerDetailsRequest;
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
        return config.get("panopta.api.templates." + credit.effectiveManagedLevel().toString()
                                                      + "." + credit.getOperatingSystem().toLowerCase());
    }

    public static class Request {
        public UUID vmId;
        public UUID orionGuid;
        public long hfsVmId;
        public String panoptaTemplates;
    }
}
