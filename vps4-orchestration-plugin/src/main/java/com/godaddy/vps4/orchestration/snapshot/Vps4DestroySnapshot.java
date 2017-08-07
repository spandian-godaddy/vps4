package com.godaddy.vps4.orchestration.snapshot;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.snapshot.DestroySnapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
        name="Vps4DestroySnapshot",
        requestType=Vps4DestroySnapshot.Request.class,
        responseType=Void.class
    )
public class Vps4DestroySnapshot extends ActionCommand<Vps4DestroySnapshot.Request, Void> {

    final SnapshotService snapshotService;

    @Inject
    public Vps4DestroySnapshot(@SnapshotActionService ActionService actionService,
            SnapshotService snapshotService) {
        super(actionService);
        this.snapshotService = snapshotService;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        context.execute("DestroySnapshot", DestroySnapshot.class, request.hfsSnapshotId);

        snapshotService.markSnapshotDestroyed(request.vps4SnapshotId);
        return null;
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsSnapshotId;
        public UUID vps4SnapshotId;

        @Override
        public long getActionId() {
            return actionId;
        }
    }

}
