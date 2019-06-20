package com.godaddy.vps4.web.dns;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class ActionCommandHelper {
    private static final Logger logger = LoggerFactory.getLogger(ActionCommandHelper.class);
    @Inject
    private ObjectMapperProvider mapperProvider;

    public VmAction createActionAndExecute(ActionService actionService,
                                                  CommandService commandService,
                                                  UUID vmId, ActionType actionType,
                                                  ActionRequest request, String commandName,
                                                  GDUser user) {
        long actionId =
                actionService.createAction(vmId, actionType, getRequestAsJsonString(request), user.getUsername());
        request.setActionId(actionId);

        CommandState command = Commands.execute(commandService, actionService, commandName, request);
        logger.info("managing vm {} with command {}:{}", vmId, actionType, command.commandId);
        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }


    private String getRequestAsJsonString(ActionRequest request) {
        try {
            return mapperProvider.get().writeValueAsString(request);
        } catch (JsonProcessingException jsonPex) {
            logger.warn("Could not convert request {} to json. ", request.toString(), jsonPex);
            return new JSONObject().toJSONString();
        }
    }
}