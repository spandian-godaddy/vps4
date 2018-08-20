package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
    name="Vps4TestCommand",
    requestType=Vps4TestCommand.TestRequest.class,
    responseType=Long.class,
    retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4TestCommand extends ActionCommand<Vps4TestCommand.TestRequest, Long> {

    @Inject
    public Vps4TestCommand(ActionService actionService) {
        super(actionService);
    }

    @Override
    public Long executeWithAction(CommandContext context, TestRequest request) {
        return request.value + 1;
    }

    public static class TestRequest implements ActionRequest {

        public long actionId;

        public long value;

        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }
}
