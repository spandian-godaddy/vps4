package com.godaddy.vps4.web.util;

import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.vm.VmAction;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class VmHelper {
    private static final Logger logger = LoggerFactory.getLogger(VmHelper.class);

    public static VmAction createActionAndExecute(ActionService actionService,
                                                  CommandService commandService,
                                                  VirtualMachineService virtualMachineService,
                                                  UUID vmId, ActionType actionType,
                                                  ActionRequest request, String commandName) {
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        long actionId = actionService.createAction(vmId, actionType, new JSONObject().toJSONString(), vps4UserId);
        request.setActionId(actionId);

        CommandState command = Commands.execute(commandService, actionService, commandName, request);
        logger.info("managing vm {} with command {}:{}", vmId, actionType, command.commandId);
        return new VmAction(actionService.getAction(actionId));
    }
}
