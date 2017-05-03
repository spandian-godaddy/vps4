package com.godaddy.vps4.orchestration.hfs.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.nodeping.NodePingAction;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class CreateCheck implements Command<CreateCheck.Request, Long> {
    
    private static final Logger logger = LoggerFactory.getLogger(CreateCheck.class);
    
    public static class Request {
        public Request(long nodePingAccountId, String target, String label) {
            this.accountId = nodePingAccountId;
            this.target = target;
            this.label = label;
        }
        public long accountId;
        public String target;
        public String label;
    }
    
    final NodePingService nodePingService;
    
    @Inject
    public CreateCheck(NodePingService nodePingService) {
        this.nodePingService = nodePingService;
    }
    
    @Override
    public Long execute(CommandContext context, CreateCheck.Request request) {
        
        logger.info("Sending HFS request to create nodeping check for target: {}", request.target);
        
        NodePingAction hfsAction = context.execute("RequestCreateNodepingCheck",
                ctx -> nodePingService.createCheck(request.accountId, request.target, request.label));
        
        hfsAction = context.execute(WaitForPingCheckAction.class, hfsAction);
        
        logger.info("Check {} created on target {}", hfsAction.checkId, request.target);

        NodePingCheck check = nodePingService.getCheck(hfsAction.accountId, hfsAction.checkId);
        
        return check.checkId;
    }
}
