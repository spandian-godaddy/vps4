package com.godaddy.vps4.cdn;

import com.godaddy.vps4.cdn.model.VmCdnSite;

import java.util.List;
import java.util.UUID;

public interface CdnDataService {

    void createCdnSite(UUID vmId, long ipAddressId, String domain, String siteId);
    VmCdnSite getCdnSiteFromId(UUID vmId, String siteId);
    List<VmCdnSite> getActiveCdnSitesOfVm(UUID vmId);

    void destroyCdnSite(UUID vmId, String siteId);
}