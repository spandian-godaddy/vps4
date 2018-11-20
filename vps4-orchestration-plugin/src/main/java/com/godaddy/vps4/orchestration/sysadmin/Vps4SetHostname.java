package com.godaddy.vps4.orchestration.sysadmin;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.cpanel.RefreshCpanelLicense;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.UpdateHostnameStep;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;

@CommandMetadata(
    name = "Vps4SetHostname",
    requestType = Vps4SetHostname.Request.class,
    retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SetHostname extends ActionCommand<Vps4SetHostname.Request, Void> {

    protected static final Logger logger = LoggerFactory.getLogger(Vps4SetHostname.class);

    private final VirtualMachineService virtualMachineService;

    @Inject
    public Vps4SetHostname(ActionService actionService, VirtualMachineService virtualMachineService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request)
            throws Exception {

        logger.debug("Setting hostname to {} on vm {}", request.setHostnameRequest.hostname, request.vmId);

        virtualMachineService.setHostname(request.vmId, request.setHostnameRequest.hostname);
        JSONObject response = new JSONObject();
        try{
            setStep(request.actionId, UpdateHostnameStep.UpdatingHostname);
            context.execute(SetHostname.class, request.setHostnameRequest);
        }catch(Exception e){
            logger.error("Error while setting hostname to {} on vm {}.", request.setHostnameRequest.hostname, request.vmId);
            virtualMachineService.setHostname(request.vmId, request.oldHostname);
            response.put("message", e.getMessage());
            throw e;
        }

        if (virtualMachineService.virtualMachineHasCpanel(request.vmId)){
            // Refresh Cpanel License
            setStep(request.actionId, UpdateHostnameStep.RefreshingCpanelLicense);
            RefreshCpanelLicense.Request cpLicRequest = new RefreshCpanelLicense.Request();
            cpLicRequest.hfsVmId = request.setHostnameRequest.hfsVmId;
            context.execute(RefreshCpanelLicense.class, cpLicRequest);
        }

        return null;
    }

    public static class ActionState{
        public UpdateHostnameStep step;
    }

    private void setStep(long actionId, UpdateHostnameStep step) throws JsonProcessingException {
        ActionState state = new ActionState();
        state.step = step;
        actionService.updateActionState(actionId, mapper.writeValueAsString(state));
    }

    public static class Request extends Vps4ActionRequest {
        public SetHostname.Request setHostnameRequest;
        public String oldHostname;
    }

}
