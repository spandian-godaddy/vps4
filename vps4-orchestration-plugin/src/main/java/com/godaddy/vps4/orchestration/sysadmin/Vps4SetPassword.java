package com.godaddy.vps4.orchestration.sysadmin;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
    name = "Vps4SetPassword",
    requestType = Vps4SetPassword.Request.class,
    responseType = Void.class
)
public class Vps4SetPassword extends ActionCommand<Vps4SetPassword.Request, Void> {

    @Inject
    public Vps4SetPassword(ActionService actionService) {
        super(actionService);
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request req)
            throws Exception {

        context.execute(SetPassword.class, req.setPasswordRequest);

        return null;
    }

    public static class Request implements ActionRequest {
        public SetPassword.Request setPasswordRequest;
        public long actionId;

        public long getActionId() {
            return actionId;
        }
    }

}
