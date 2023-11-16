package com.godaddy.vps4.firewall.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.FirewallSite;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class JdbcFirewallService implements FirewallService {

    private final DataSource dataSource;

    @Inject
    public JdbcFirewallService(DataSource dataSource) {
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
    public List<FirewallSite> getActiveFirewallSitesOfVm(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT vfs.domain, vfs.site_id, vfs.valid_until as \"vfs_valid_until\"," +
                        " vfs.valid_on as \"vfs_valid_on\", vfs.vm_id as \"vfs_vm_id\", ia.*, family(ia.ip_address)" +
                        " FROM vm_firewall_site vfs join ip_address ia on vfs.ip_address_id = ia.address_id" +
                        " WHERE vfs.vm_id = ? AND vfs.valid_until='infinity' ORDER BY vfs.valid_on",
                Sql.listOf(FirewallSiteMapper::mapFirewallSite),
                vmId);
    }

    @Override
    public FirewallSite getFirewallSiteFromId(String siteId) {
        return Sql.with(dataSource).exec(
                "SELECT vfs.domain, vfs.site_id, vfs.valid_until as \"vfs_valid_until\"," +
                        " vfs.valid_on as \"vfs_valid_on\", vfs.vm_id as \"vfs_vm_id\", ia.*, family(ia.ip_address)" +
                        " FROM vm_firewall_site vfs join ip_address ia on vfs.ip_address_id = ia.address_id" +
                        " WHERE vfs.site_id = ? ORDER BY vfs.valid_on",
                Sql.nextOrNull(FirewallSiteMapper::mapFirewallSite),
                siteId);
    }

    @Override
    public void destroyFirewallSite(String siteId) {
        Sql.with(dataSource).exec("UPDATE vm_firewall_site SET valid_until = now_utc() WHERE site_id = ?",
                null, siteId);
    }
}
