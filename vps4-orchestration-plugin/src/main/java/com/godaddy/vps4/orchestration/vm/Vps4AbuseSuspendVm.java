package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


@CommandMetadata(
        name="Vps4AbuseSuspendVm",
        requestType=VmActionRequest.class,
        responseType= Vps4AbuseSuspendVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4AbuseSuspendVm extends ActionCommand<VmActionRequest, Vps4AbuseSuspendVm.Response> {

    private final Logger logger = LoggerFactory.getLogger(Vps4AbuseSuspendVm.class);

    final ActionService actionService;
    final VmService vmService;
    final CreditService creditService;

    @Inject
    public Vps4AbuseSuspendVm(ActionService actionService, VmService vmService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
        this.creditService = creditService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) throws Exception {
        logger.info("Request: {}", request);
        setAccountStatusToAbuseSuspend(request);
        VmAction hfsAction = stopVm(context, request);
        return getResponse(request, hfsAction);
    }

    private void setAccountStatusToAbuseSuspend(VmActionRequest request) {
        creditService.setStatus(request.virtualMachine.orionGuid, AccountStatus.ABUSE_SUSPENDED);
    }

    private VmAction stopVm(CommandContext context, VmActionRequest request) {
        VmAction hfsAction = context.execute("Vps4StopVm", ctx -> vmService.stopVm(request.virtualMachine.hfsVmId), VmAction.class);
        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);
        return hfsAction;
    }

    private Response getResponse(VmActionRequest request, VmAction hfsAction) {
        Response response = new Response();
        response.vmId = request.virtualMachine.hfsVmId;
        response.hfsAction = hfsAction;
        logger.info("Response: {}", response);
        return response;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

}
