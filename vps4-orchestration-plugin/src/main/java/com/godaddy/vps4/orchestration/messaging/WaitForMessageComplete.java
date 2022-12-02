package com.godaddy.vps4.orchestration.messaging;

import com.google.inject.Inject;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class WaitForMessageComplete implements Command<String, Void> {
    //This class was deprecated because we were overwhelming the messaging api. It was kept to keep the orchestration engine
    //happy. At some point in the future when we are sure there are no more of these commands to execute it can be removed.

    private static final Logger logger = LoggerFactory.getLogger(WaitForMessageComplete.class);

    @Inject
    public WaitForMessageComplete() { }

    @Override
    public Void execute(CommandContext context, String messageId) {
        logger.error("Attempted to poll the status of message ID {} will not continue. Please do not use this command.", messageId);

        return null;
    }
}
