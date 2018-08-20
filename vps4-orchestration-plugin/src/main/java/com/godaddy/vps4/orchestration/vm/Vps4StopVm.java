package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import gdg.hfs.orchestration.CommandRetryStrategy;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;


@CommandMetadata(
        name="Vps4StopVm",
        requestType=VmActionRequest.class,
        responseType=Vps4StopVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4StopVm extends ActionCommand<VmActionRequest, Vps4StopVm.Response> {

    private final Logger logger = LoggerFactory.getLogger(Vps4StopVm.class);

    final ActionService actionService;
    final VmService vmService;

    @Inject
    public Vps4StopVm(ActionService actionService, VmService vmService) {
        super(actionService);
        this.actionService = actionService;
        this.vmService = vmService;
    }

    @Override
    protected Response executeWithAction(CommandContext context, VmActionRequest request) throws Exception {

        logger.info("Request: {}", request);
        VmAction hfsAction = context.execute("Vps4StopVm", ctx -> {
            return vmService.stopVm(request.virtualMachine.hfsVmId);
        }, VmAction.class);

        hfsAction = context.execute(WaitForManageVmAction.class, hfsAction);

        Vps4StopVm.Response response = new Vps4StopVm.Response();
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
