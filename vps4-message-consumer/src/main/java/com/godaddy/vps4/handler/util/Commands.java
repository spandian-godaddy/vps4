package com.godaddy.vps4.handler.util;

import java.util.Arrays;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Commands {

    private static final Logger logger = LoggerFactory.getLogger(Commands.class);

    public static CommandState execute(CommandService commandService, String commandName, Object request) {

        CommandGroupSpec groupSpec = new CommandGroupSpec();

        CommandSpec commandSpec = new CommandSpec();
        commandSpec.command = commandName;
        commandSpec.request = request;
        groupSpec.commands = Arrays.asList(commandSpec);

        logger.info("CommandSpec: {}", commandSpec);
        logger.info("CommandGroupSpec: {}", groupSpec);

        return commandService.executeCommand(groupSpec);
    }
    
    public static CommandState execute(CommandService commandService, ActionService actionService, String commandName, ActionRequest request) {
        CommandState command = execute(commandService, commandName, request);
        actionService.tagWithCommand(request.getActionId(), command.commandId);
        return command;
    }

}
