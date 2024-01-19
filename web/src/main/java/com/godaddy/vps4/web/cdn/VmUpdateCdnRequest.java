package com.godaddy.vps4.web.cdn;

import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;

public class VmUpdateCdnRequest {
    public CdnBypassWAF bypassWAF;
    public String cacheLevel;

    // Empty constructor required for Jackson
    public VmUpdateCdnRequest() {}

    public VmUpdateCdnRequest(CdnBypassWAF bypassWAF, String cacheLevel) {
        this.bypassWAF = bypassWAF;
        this.cacheLevel = cacheLevel;
    }
}
