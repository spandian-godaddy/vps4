package com.godaddy.vps4.orchestration.sysadmin;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

@CommandMetadata(
    name = "Vps4ToggleAdmin",
    requestType = Vps4ToggleAdmin.Request.class,
    responseType = Void.class
)
public class Vps4ToggleAdmin extends ActionCommand<Vps4ToggleAdmin.Request, Void> {

    protected static final Logger logger = LoggerFactory.getLogger(Vps4ToggleAdmin.class);

    private final SysAdminService sysAdminService;

    private final VmUserService userService;

    @Inject
    public Vps4ToggleAdmin(ActionService actionService, SysAdminService sysAdminService, VmUserService userService) {
        super(actionService);
        this.sysAdminService = sysAdminService;
        this.userService = userService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request)
            throws Exception {

        logger.debug("Setting admin access to {} for user {} in vm {}", request.enabled, request.username, request.hfsVmId);

        SysAdminAction sysAdminHfsAction = request.enabled
                ? context.execute("EnableAdminHfs", ctx -> sysAdminService.enableAdmin(request.hfsVmId, request.username))
                : context.execute("DisableAdminHfs", ctx -> sysAdminService.disableAdmin(request.hfsVmId, request.username));

        context.execute(WaitForSysAdminAction.class, sysAdminHfsAction);

        userService.updateUserAdminAccess(request.username, request.vmId, request.enabled);

        return null;

    }

    public static class Request implements ActionRequest {
        public boolean enabled;
        public long hfsVmId;
        public UUID vmId;
        public String username;
        public long actionId;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }
    public static class Response {
        public long hfsVmId;
        public SysAdminAction hfsAction;
    }
    
}
