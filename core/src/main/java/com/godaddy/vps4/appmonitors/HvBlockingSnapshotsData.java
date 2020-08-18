package com.godaddy.vps4.appmonitors;

import java.time.Instant;
import java.util.UUID;

public class HvBlockingSnapshotsData {
    public String hypervisor;
    public UUID vmId;
    public Instant created;

    public HvBlockingSnapshotsData(String hypervisor, UUID vmId, Instant created) {
        this.hypervisor = hypervisor;
        this.vmId = vmId;
        this.created = created;
    }
}
