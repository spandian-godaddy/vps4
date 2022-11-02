package com.godaddy.vps4.orchestration.sysadmin;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

import javax.inject.Inject;

@CommandMetadata(
    name = "Vps4SetSupportUserPassword",
    requestType = Vps4SetSupportUserPassword.Request.class,
    retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SetSupportUserPassword extends ActionCommand<Vps4SetSupportUserPassword.Request, Void> {

    @Inject
    public Vps4SetSupportUserPassword(ActionService actionService) {
        super(actionService);
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request req) {

        context.execute(SetPassword.class, req.setPasswordRequest);

        return null;
    }

    public static class Request extends Vps4ActionRequest {
        public SetPassword.Request setPasswordRequest;
    }

}
