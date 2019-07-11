package com.godaddy.vps4.orchestration.dns;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.vps4.vm.ActionStatus;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class WaitForDnsAction implements Command<HfsDnsAction, HfsDnsAction> {

    private static final Logger
            logger = LoggerFactory.getLogger(com.godaddy.vps4.orchestration.vm.WaitForManageVmAction.class);

    private final HfsDnsService hfsDnsService;

    @Inject
    public WaitForDnsAction(HfsDnsService dnsService) {
        this.hfsDnsService = dnsService;
    }

    @Override
    public HfsDnsAction execute(CommandContext context, HfsDnsAction hfsDnsAction) {
        // wait for action to complete
        while (hfsDnsAction.status == ActionStatus.NEW
                || hfsDnsAction.status == ActionStatus.IN_PROGRESS) {

            logger.debug("waiting for dns action to complete: {}", hfsDnsAction);

            context.sleep(2000);

            hfsDnsAction = hfsDnsService.getDnsAction(hfsDnsAction.dns_action_id);
        }
        if (hfsDnsAction.status == ActionStatus.COMPLETE) {
            logger.info("HFS DNS Action completed. hfsAction: {} ", hfsDnsAction);
        } else {
            throw new RuntimeException(String.format(" Failed DNS action: %s", hfsDnsAction));
        }
        return hfsDnsAction;
    }
}
