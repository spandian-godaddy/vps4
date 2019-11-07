package com.godaddy.vps4.orchestration.panopta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.sysadmin.WaitForSysAdminAction;
import com.google.inject.Inject;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

@CommandMetadata(
        name="GetPanoptaServerKeyFromHfs",
        requestType= Long.class,
        responseType= SysAdminAction.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class GetPanoptaServerKeyFromHfs implements Command<Long, SysAdminAction> {
    public static final Logger logger = LoggerFactory.getLogger(GetPanoptaServerKeyFromHfs.class);
    private SysAdminService sysAdminService;

    @Inject
    public GetPanoptaServerKeyFromHfs(SysAdminService sysAdminService) {
        this.sysAdminService = sysAdminService;
    }

    @Override
    public SysAdminAction execute(CommandContext context, Long hfsVmId) {
        logger.info("Getting panopta server key from HFS for hfs vm id {} ", hfsVmId);

        SysAdminAction hfsSysAction = context.execute("HfsGetPanoptaServerKey",
                                                      ctx -> sysAdminService.getPanoptaServerKey(hfsVmId),
                                                      SysAdminAction.class);
        hfsSysAction = context.execute("WaitForPanoptaServerKeyFromHfs-" + hfsVmId, WaitForSysAdminAction.class, hfsSysAction);

        return hfsSysAction;
    }
}
