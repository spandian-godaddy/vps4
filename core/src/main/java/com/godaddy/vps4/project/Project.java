package com.godaddy.vps4.project;

import java.time.Instant;

public class Project {

    private long projectId;
    private String name;
    private String vhfsSgid;
    private Instant validOn;
    private Instant validUntil;
    private long vps4UserId;

    public Project() {}
    
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
