package com.godaddy.vps4.web.cdn;

import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;

public class VmCreateCdnRequest {

    public String domain;
    public String ipAddress;
    public CdnBypassWAF bypassWAF;
    public CdnCacheLevel cacheLevel;

    // Empty constructor required for Jackson
    public VmCreateCdnRequest() {}

    public VmCreateCdnRequest(CdnBypassWAF bypassWAF, CdnCacheLevel cacheLevel) {
        this.bypassWAF = bypassWAF;
        this.cacheLevel = cacheLevel;
    }
}
