package com.godaddy.vps4.orchestration.hfs.plesk;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class ConfigurePlesk implements Command<ConfigurePlesk.ConfigurePleskRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurePlesk.class);

    final PleskService pleskService;
    
    @Inject
    public ConfigurePlesk(PleskService pleskService) {
        this.pleskService = pleskService;
    }
    
    @Override
    public Void execute(CommandContext context, ConfigurePleskRequest request) {
        logger.info("sending HFS request to config Plesk image for vmId {}", request.vmId);

        PleskAction hfsAction = context.execute("RequestFromHFS", ctx -> {
            return pleskService.imageConfig(request.vmId, request.username, request.password);
        });

        context.execute(WaitForPleskAction.class, hfsAction);

        logger.info("Completed configuring Plesk vm action {} ", hfsAction);
        return null;
    }

    public static class ConfigurePleskRequest {
        long vmId;
        String username;
        String password;
        
        public ConfigurePleskRequest(long vmId, String username, String password) {
            this.vmId = vmId;
            this.username = username;
            this.password = password;
        }
    }

}


