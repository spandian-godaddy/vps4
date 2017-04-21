package com.godaddy.vps4.handler;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.handler.util.Commands;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;

public class Vps4MessageHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(Vps4MessageHandler.class);

    private JSONParser parser = new JSONParser();

    private final VirtualMachineService virtualMachineService;
    private final CreditService creditService;
    private final ActionService actionService;
    private final Vps4UserService vps4UserService;
    private final CommandService commandService;
    private final Config config;
    private final long pingCheckAccountId;

    @Inject
    public Vps4MessageHandler(VirtualMachineService virtualMachineService, 
            CreditService creditService, 
            ActionService actionService,
            Vps4UserService vps4UserService,
            CommandService commandService,
            Config config) {
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.actionService = actionService;
        this.vps4UserService = vps4UserService;
        this.commandService = commandService;
        this.config = config;

        pingCheckAccountId = Long.parseLong(this.config.get("nodeping.accountid"));
    }

    @Override
    public void handleMessage(String message) throws MessageHandlerException {
        logger.info("Consumed message: {} ", message);
        Vps4Message vps4Message;

        try {
            JSONObject obj = (JSONObject) parser.parse(message);
            vps4Message = new Vps4Message(obj);
        }
        catch (ParseException e) {
            throw new MessageHandlerException("Can't parse message JSON", e);
        }
        catch (IllegalArgumentException e) {
            throw new MessageHandlerException("Message values are the wrong type", e);
        }

        VirtualMachine vm = virtualMachineService.getVirtualMachineByOrionGuid(vps4Message.accountGuid);
        if (vm == null) {
            logger.debug("Account {} not found, message handling will not continue", vps4Message.accountGuid);
            return;
        }

        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vps4Message.accountGuid);

        if (vm.accountStatus != credit.accountStatus) {
            switch (credit.accountStatus) {
            case ABUSE_SUSPENDED:
                // TODO: Call suspend when implemented
                break;
            case ACTIVE:
                // TODO: Call unsuspend when implemented
                break;
            case REMOVED:
                destroyVm(vm, credit);
                break;
            case SUSPENDED:
                // TODO: Call suspend when implemented
                break;
            default:
                break;

            }
        }
    }

    private void destroyVm(VirtualMachine vm, VirtualMachineCredit credit) {
        Vps4User user = vps4UserService.getUser(credit.shopperId);

        long actionId = actionService.createAction(vm.vmId, ActionType.DESTROY_VM, new JSONObject().toJSONString(),
                user.getId());

        Vps4DestroyVm.Request request = new Vps4DestroyVm.Request();
        request.hfsVmId = vm.hfsVmId;
        request.actionId = actionId;
        request.pingCheckAccountId = pingCheckAccountId;

        Commands.execute(commandService, actionService, "Vps4DestroyVm", request);
    }
}
