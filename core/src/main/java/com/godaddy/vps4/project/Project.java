package com.godaddy.vps4.project;

import java.time.Instant;

public class Project {

    private final long projectId;
    private final String name;
    private final String vhfsSgid;
    private final Instant validOn;
    private final Instant validUntil;
    private final long vps4UserId;
    
    public Project(long projectId,
                   String name,
                   String vhfsSgid,
                   Instant validOn,
                   Instant validUntil,
                   long vps4UserId) {

        this.projectId = projectId;
        this.name = name;
        this.vhfsSgid = vhfsSgid;
        this.validOn = validOn;
        this.validUntil = validUntil;
        this.vps4UserId = vps4UserId;
    }

    public long getProjectId() {return projectId;}

    public String getName() {return name;}

    public String getVhfsSgid() {return vhfsSgid;}

    public Instant getValidOn() {
        return validOn;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public long getVps4UserId() { return vps4UserId; }
}
