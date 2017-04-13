package com.godaddy.vps4.orchestration.hfs.pingcheck;

    import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.nodeping.NodePingAction;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class DeleteCheck implements Command<DeleteCheck.Request, NodePingAction>{

    private static final Logger logger = LoggerFactory.getLogger(DeleteCheck.class);

    public static class Request {
        public Request(long accountId, long checkId) {
            this.accountId = accountId;
            this.checkId = checkId;
        }
        public long accountId;
        public long checkId;
    }

    final NodePingService nodePingService;

    @Inject
    public DeleteCheck(NodePingService nodePingService) {
        this.nodePingService = nodePingService;
    }

    @Override
    public NodePingAction execute(CommandContext context, DeleteCheck.Request request) {
        
        logger.info("Sending HFS request to delete nodeping check: {}", request.checkId);
        
        NodePingAction hfsAction = context.execute("RequestDeleteNodepingCheck",
                ctx -> nodePingService.deleteCheck(request.accountId, request.checkId));
        
        hfsAction = context.execute(WaitForNodePingAction.class, hfsAction);
        
        return hfsAction;
    }
}