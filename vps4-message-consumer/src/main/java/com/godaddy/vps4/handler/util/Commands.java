package com.godaddy.vps4.handler.util;

import java.util.Arrays;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;

public class Commands {

    public static CommandState execute(CommandService commandService, String commandName, Object request) {

        CommandGroupSpec groupSpec = new CommandGroupSpec();

        CommandSpec commandSpec = new CommandSpec();
        commandSpec.command = commandName;
        commandSpec.request = request;
        groupSpec.commands = Arrays.asList(commandSpec);

        return commandService.executeCommand(groupSpec);
    }
    
    public static CommandState execute(CommandService commandService, ActionService actionService, String commandName, ActionRequest request) {
        CommandState command = execute(commandService, commandName, request);
        actionService.tagWithCommand(request.getActionId(), command.commandId);
        return command;
    }

}
