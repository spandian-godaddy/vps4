package com.godaddy.vps4.orchestration.snapshot;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.snapshot.SnapshotAction;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.scheduler.Utils;

public class WaitForSnapshotAction implements Command<SnapshotAction, SnapshotAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForSnapshotAction.class);

    private final SnapshotService hfsSnapshotService;

    @Inject
    public WaitForSnapshotAction(SnapshotService hfsSnapshotService) {
        this.hfsSnapshotService = hfsSnapshotService;
    }

    @Override
    public SnapshotAction execute(CommandContext context, SnapshotAction hfsAction) {

        long actionId = hfsAction.actionId;

        // wait for VmAction to complete
        while (hfsAction.status == SnapshotAction.Status.NEW
                || hfsAction.status == SnapshotAction.Status.IN_PROGRESS) {

            logger.debug("waiting for snapshot action to complete: {}", hfsAction);

            hfsAction = Utils.runWithRetriesForServerErrorException(context, logger, () ->{
                return hfsSnapshotService.getSnapshotAction(actionId);
            });
        }
        if(hfsAction.status == SnapshotAction.Status.COMPLETE) {
            logger.info("Snapshot Action completed. hfsAction: {} ", hfsAction );
        } else {
            throw new RuntimeException(String.format(" Failed action: %s", hfsAction));
        }
        return hfsAction;
    }

}
