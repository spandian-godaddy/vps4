package com.godaddy.vps4.web.util;

import java.util.Collections;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.util.ThreadLocalRequestId;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;

import net.minidev.json.JSONObject;

public class Commands {
    private static final Logger logger = LoggerFactory.getLogger(Commands.class);

    public static CommandState execute(CommandService commandService, String commandName, Object request) {
        CommandGroupSpec groupSpec = new CommandGroupSpec();

        CommandSpec commandSpec = new CommandSpec();
        commandSpec.command = commandName;
        commandSpec.request = request;
        groupSpec.commands = Collections.singletonList(commandSpec);

        CommandState command = commandService.executeCommand(groupSpec);

        if (command.commandId != null) {
            String requestId = ThreadLocalRequestId.get();
            if (requestId != null) {
                logger.info("request {} => command {}", requestId, command.commandId);
            }
        }

        return command;
    }

    public static CommandState execute(CommandService commandService, ActionService actionService, String commandName, ActionRequest request) {
        CommandState command = null;

        try {
            command = execute(commandService, commandName, request);
        } catch (Exception e) {
            JSONObject response = new JSONObject();
            response.put("message", "Failed to execute command");
            actionService.failAction(request.getActionId(), response.toJSONString(), null);
        }

        if (command != null) {
            actionService.tagWithCommand(request.getActionId(), command.commandId);
        }
        return command;
    }

    public static boolean cancel(CommandService commandService, UUID commandId) {
        return commandService.cancel(commandId);
    }
}
