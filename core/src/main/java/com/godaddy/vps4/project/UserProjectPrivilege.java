package com.godaddy.vps4.project;

import java.time.Instant;

public class UserProjectPrivilege {

    public int privilegeId;
    public long projectId;
    public long vps4UserId;
    public Instant validOn;
    public Instant validUntil;

    public UserProjectPrivilege(int privilegeId,
                                long projectId,
                                long vps4UserId,
                                Instant validOn,
                                Instant validUntil) {

        this.projectId = projectId;
        this.privilegeId = privilegeId;
        this.vps4UserId = vps4UserId;
        this.validOn = validOn;
        this.validUntil = validUntil;

    }

}
