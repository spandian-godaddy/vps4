package com.godaddy.vps4.orchestration.hfs.sysadmin;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin.Request;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class ToggleAdmin implements Command<Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(ToggleAdmin.class);

    final SysAdminService sysAdminService;

    @Inject
    public ToggleAdmin(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {

        try {
            // Paul Tanganelli [10/28/16] (HFS) Looks like calling disableAdmin before enableAdmin causes problems.
            logger.debug("Setting admin access to {} for user {} in vm {}", request.enabled, request.username, request.vmId);

            SysAdminAction enableAction = context.execute("EnabledAdminHfs",
                    ctx -> sysAdminService.enableAdmin(request.vmId, request.username),
                    SysAdminAction.class);

            context.execute("WaitForEnableAction", WaitForSysAdminAction.class, enableAction);

            if (!request.enabled) {
                SysAdminAction disableAction = context.execute("DisableAdminHfs",
                        ctx -> sysAdminService.disableAdmin(request.vmId, request.username),
                        SysAdminAction.class);

                context.execute("WaitForDisableAction", WaitForSysAdminAction.class, disableAction);
            }

            // FIXME update VPS4 database
            // userService.updateUserAdminAccess(action.getUsername(), action.getVmId(), enabled);

            // action.status = ActionStatus.COMPLETE;

        } catch (Exception e) {
//            String toggle = enabled ? "ENABLE" : "DISABLE";
//            String message = String.format("Failed to %s admin for vm: %d", toggle, vmId);
//            action.status = ActionStatus.ERROR;
//            logger.error(message, e);
//            throw new Vps4Exception(toggle+"_ADMIN_WORKER_FAILED", message);
        }

        return null;
    }

    public static class Request {
        public boolean enabled;
        public long vmId;
        public String username;
    }
}
