package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VmMetric;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

public class AddAdditionalFqdnPanopta implements Command<AddAdditionalFqdnPanopta.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(AddAdditionalFqdnPanopta.class);

    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;

    private CommandContext context;
    private Request request;

    @Inject
    public AddAdditionalFqdnPanopta(PanoptaDataService panoptaDataService, PanoptaService panoptaService) {
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        this.context = context;
        this.request = request;
        this.updateAdditionalFqdnList();
        this.addMonitoringCheckToFqdn();
        this.updateDatabase();
        return null;
    }

    public void updateDatabase() {
        panoptaDataService.addPanoptaAdditionalFqdn(request.additionalFqdn, panoptaService.getServer(request.vmId).serverId);
    }

    public void updateAdditionalFqdnList() {
        logger.info("Attempting to add additional fqdn {} to vmId {} in panopta.", request.additionalFqdn, request.vmId);
        try {
            panoptaService.addAdditionalFqdnToServer(request.vmId, request.additionalFqdn);
        }
        catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMonitoringCheckToFqdn() {
        logger.info("Attempting to add monitoring check to fqdn {}.", request.additionalFqdn);
        try {
            panoptaService.addNetworkService(request.vmId, request.isHttps ? VmMetric.HTTPS : VmMetric.HTTP,
                    request.additionalFqdn, request.operatingSystemId, request.isManaged, request.hasMonitoring);
        }
        catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }
    public static class Request {
        public UUID vmId;
        public String additionalFqdn;
        public boolean isHttps;
        public int operatingSystemId;
        public boolean isManaged;
        public boolean hasMonitoring;
    }
}