package com.godaddy.vps4.orchestration.hfs.snapshot;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.snapshot.WaitForSnapshotAction;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.snapshot.SnapshotAction;
import gdg.hfs.vhfs.snapshot.SnapshotService;

public class DestroySnapshot implements Command<Long, Void> {

    final SnapshotService hfsSnapshotService;

    @Inject
    public DestroySnapshot(SnapshotService hfsSnapshotService) {
        this.hfsSnapshotService = hfsSnapshotService;
    }

    @Override
    public Void execute(CommandContext context, Long snapshotId) {

        SnapshotAction hfsAction = context.execute("DestroySnapshotHfs",
                ctx -> hfsSnapshotService.destroySnapshot(snapshotId));

        context.execute(WaitForSnapshotAction.class, hfsAction);

        return null;
    }

}
