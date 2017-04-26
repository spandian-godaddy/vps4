package com.godaddy.vps4.project;

import java.time.Instant;

public class Project {

    private final long projectId;
    private final String name;
    private final String vhfsSgid;
    private final Instant validOn;
    private final Instant validUntil;
    
    public Project(long projectId,
            String name,
            String vhfsSgid,
            Instant validOn,
            Instant validUntil) {

        this.projectId = projectId;
        this.name = name;
        this.vhfsSgid = vhfsSgid;
        this.validOn = validOn;
        this.validUntil = validUntil;

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

}
