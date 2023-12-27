package com.godaddy.vps4.cdn.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.VmCdnSite;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class JdbcCdnDataService implements CdnDataService {

    private final DataSource dataSource;

    @Inject
    public JdbcCdnDataService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createCdnSite(UUID vmId, long ipAddressId, String domain, String siteId) {
        Sql.with(dataSource).exec("INSERT INTO vm_cdn_site (vm_id, ip_address_id, domain, site_id) " +
                        "VALUES (?, ?, ?, ?) ",
                null,
                vmId, ipAddressId, domain, siteId);

    }

    @Override
    public List<VmCdnSite> getActiveCdnSitesOfVm(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT vcs.domain, vcs.site_id, vcs.valid_until as \"vcs_valid_until\"," +
                        " vcs.valid_on as \"vcs_valid_on\", vcs.vm_id as \"vcs_vm_id\", ia.*, family(ia.ip_address)" +
                        " FROM vm_cdn_site vcs join ip_address ia on vcs.ip_address_id = ia.address_id" +
                        " WHERE vcs.vm_id = ? AND vcs.valid_until='infinity' ORDER BY vcs.valid_on",
                Sql.listOf(CdnSiteMapper::mapCdnSite),
                vmId);
    }

    @Override
    public VmCdnSite getCdnSiteFromId(UUID vmId, String siteId) {
        return Sql.with(dataSource).exec(
                "SELECT vcs.domain, vcs.site_id, vcs.valid_until as \"vcs_valid_until\"," +
                        " vcs.valid_on as \"vcs_valid_on\", vcs.vm_id as \"vcs_vm_id\", ia.*, family(ia.ip_address)" +
                        " FROM vm_cdn_site vcs join ip_address ia on vcs.ip_address_id = ia.address_id" +
                        " WHERE vcs.site_id = ? AND vcs.vm_id = ? ORDER BY vcs.valid_on",
                Sql.nextOrNull(CdnSiteMapper::mapCdnSite),
                siteId, vmId);
    }

    @Override
    public void destroyCdnSite(UUID vmId, String siteId) {
        Sql.with(dataSource).exec("UPDATE vm_cdn_site SET valid_until = now_utc() WHERE vm_id = ? AND site_id = ?",
                null, vmId, siteId);
    }
}
