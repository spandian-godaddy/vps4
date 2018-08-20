package com.godaddy.vps4.orchestration.snapshot;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.hfs.snapshot.DestroySnapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4DestroySnapshot",
        requestType=Vps4DestroySnapshot.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
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

    public static class Request extends Vps4ActionRequest {
        public long hfsSnapshotId;
        public UUID vps4SnapshotId;
    }

}
