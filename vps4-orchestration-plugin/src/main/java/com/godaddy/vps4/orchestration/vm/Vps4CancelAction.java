package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@CommandMetadata(
        name="Vps4CancelAction",
        requestType=Long.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4CancelAction implements Command<Long, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4CancelAction.class);

    final ActionService actionService;

    @Inject
    public Vps4CancelAction(ActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public Void execute(CommandContext commandContext, Long actionId) {
        // NOTE: THIS IS JUST AN EXAMPLE IMPLEMENTATION OF A CANCEL COMMAND. a REAL IMPLEMENTATION WOULD ACTUALLY DO
        // SOME REAL CLEANUP/ROLLBACK AS PART OF THE EXECUTE!!!!!
        logger.info("Processing cancel of action: {}", actionId);
        return null;
    }
}
