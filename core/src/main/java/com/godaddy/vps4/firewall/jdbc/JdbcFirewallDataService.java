package com.godaddy.vps4.firewall.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.model.VmFirewallSite;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class JdbcFirewallDataService implements FirewallDataService {

    private final DataSource dataSource;

    @Inject
    public JdbcFirewallDataService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createFirewallSite(UUID vmId, long ipAddressId, String domain, String siteId) {
        Sql.with(dataSource).exec("INSERT INTO vm_firewall_site (vm_id, ip_address_id, domain, site_id) " +
                        "VALUES (?, ?, ?, ?) ",
                null,
                vmId, ipAddressId, domain, siteId);

    }

    @Override
    public List<VmFirewallSite> getActiveFirewallSitesOfVm(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT vfs.domain, vfs.site_id, vfs.valid_until as \"vfs_valid_until\"," +
                        " vfs.valid_on as \"vfs_valid_on\", vfs.vm_id as \"vfs_vm_id\", ia.*, family(ia.ip_address)" +
                        " FROM vm_firewall_site vfs join ip_address ia on vfs.ip_address_id = ia.address_id" +
                        " WHERE vfs.vm_id = ? AND vfs.valid_until='infinity' ORDER BY vfs.valid_on",
                Sql.listOf(FirewallSiteMapper::mapFirewallSite),
                vmId);
    }

    @Override
    public VmFirewallSite getFirewallSiteFromId(UUID vmId, String siteId) {
        return Sql.with(dataSource).exec(
                "SELECT vfs.domain, vfs.site_id, vfs.valid_until as \"vfs_valid_until\"," +
                        " vfs.valid_on as \"vfs_valid_on\", vfs.vm_id as \"vfs_vm_id\", ia.*, family(ia.ip_address)" +
                        " FROM vm_firewall_site vfs join ip_address ia on vfs.ip_address_id = ia.address_id" +
                        " WHERE vfs.site_id = ? AND vfs.vm_id = ? ORDER BY vfs.valid_on",
                Sql.nextOrNull(FirewallSiteMapper::mapFirewallSite),
                siteId, vmId);
    }

    @Override
    public void destroyFirewallSite(String siteId) {
        Sql.with(dataSource).exec("UPDATE vm_firewall_site SET valid_until = now_utc() WHERE site_id = ?",
                null, siteId);
    }
}
