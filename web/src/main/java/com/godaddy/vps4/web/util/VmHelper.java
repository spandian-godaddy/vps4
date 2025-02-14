package com.godaddy.vps4.web.util;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmHelper {
    private static final Logger logger = LoggerFactory.getLogger(VmHelper.class);

    public static VmAction createActionAndExecute(ActionService actionService,
                                                  CommandService commandService,
                                                  UUID vmId, ActionType actionType,
                                                  ActionRequest request, String commandName,
                                                  GDUser user) {
        long actionId = actionService.createAction(vmId, actionType, new JSONObject().toJSONString(), user.getUsername());
        request.setActionId(actionId);

        CommandState command = Commands.execute(commandService, actionService, commandName, request);
        if (command == null) {
            logger.error("Failed to create command for VM {}", vmId);
        } else {
            logger.info("Managing VM {} with command {}: {}", vmId, actionType, command.commandId);
        }
        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }
}
