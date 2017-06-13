package com.godaddy.vps4.orchestration;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.util.ThreadLocalRequestId;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public abstract class ActionCommand<Req extends ActionRequest, Res> implements Command<Req, Res> {

    private static final Logger logger = LoggerFactory.getLogger(ActionCommand.class);

    protected static final ObjectMapper mapper = new ObjectMapper();

    protected final ActionService actionService;

    public ActionCommand(ActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public Res execute(CommandContext context, Req req) {

        final long actionId = req.getActionId();

        // TODO validate actionId

        setRequestId(context);

        actionService.markActionInProgress(actionId);
        try {
            Res response = executeWithAction(context, req);

            actionService.completeAction(actionId, mapper.writeValueAsString(response), null);

            return response;

        } catch (Exception e) {

            // TODO get stack trace, etc from exception and put it in hidden action field
            JSONObject response = new JSONObject();
            response.put("message", e.getMessage());

            actionService.failAction(actionId, response.toJSONString(), null);
            throw new RuntimeException(e);

        } finally {
            ThreadLocalRequestId.set(null);
        }
    }

    protected abstract Res executeWithAction(CommandContext context, Req req) throws Exception;

    /**
     * peek inside the implementation of a CommandContext and
     * pull out the command ID, then set it as the request ID
     *
     * @param context
     */
    public static void setRequestId(CommandContext context) {
        ThreadLocalRequestId.set( context.getId().toString() );
    }

}
