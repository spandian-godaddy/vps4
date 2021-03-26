package com.godaddy.vps4.orchestration.hfs.cpanel;

import javax.ws.rs.ClientErrorException;

import com.godaddy.hfs.cpanel.CPanelAction;
import com.godaddy.hfs.cpanel.CPanelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;


public class UnlicenseCpanel implements Command<Long, Void> {

    private CPanelService cpanelService;

    @Inject
    public UnlicenseCpanel(CPanelService cpanelService) {
        this.cpanelService = cpanelService;
    }

    private static final Logger logger = LoggerFactory.getLogger(UnlicenseCpanel.class);

    @Override
    public Void execute(CommandContext context, Long hfsVmId) {
        if(!vmHasCpanelLicense(hfsVmId)) {
            logger.warn("No license for vm {} found.", hfsVmId);
            return null;
        }
        CPanelAction action = context.execute("Unlicense-Cpanel",
                ctx -> cpanelService.licenseRelease(null, hfsVmId),
                CPanelAction.class);
        context.execute(WaitForCpanelAction.class, action);
        return null;
    }
    private boolean vmHasCpanelLicense(Long hfsVmId){
        try {
            return cpanelService.getLicenseFromDb(hfsVmId).licensedIp != null;
        }
        catch(ClientErrorException e){
            // The only cases of exceptions we've seen is when hfs can't find an ip associated with
            // the VM.  They then throw a 422 (VM does not have a resource ID associated with it)
            logger.warn("Exception returned when checking for cpanel db license: {}", e);
            return false;
        }
    }
}
