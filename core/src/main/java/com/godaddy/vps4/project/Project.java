package com.godaddy.vps4.project;

import java.time.Instant;
import java.util.UUID;

public class Project {

    private final long sgid;
    private final String name;
    private final String vhfsSgid;
//    private final java.util.UUID billingAccountUuid;
    private final int dataCenterId;
    private final Instant validOn;
    private final Instant validUntil;
    
    public Project(long sgid,
            String name,
            String vhfsSgid,
//            java.util.UUID billingAccountUuid,
            int dataCenterId,
            Instant validOn,
            Instant validUntil) {

        this.sgid = sgid;
        this.name = name;
        this.vhfsSgid = vhfsSgid;
//        this.billingAccountUuid = billingAccountUuid;
        this.dataCenterId = dataCenterId;
        this.validOn = validOn;
        this.validUntil = validUntil;

    }

    public long getSgid() {return sgid;}

    public String getName() {return name;}

    public String getVhfsSgid() {return vhfsSgid;}

    public int getDataCenterId() {return dataCenterId;}

//    public UUID getBillingAccountUuid() {return billingAccountUuid; }

    public Instant getValidOn() {
        return validOn;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

}
