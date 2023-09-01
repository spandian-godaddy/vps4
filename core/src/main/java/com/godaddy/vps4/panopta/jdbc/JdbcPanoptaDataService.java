package com.godaddy.vps4.panopta.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.util.TimestampUtils;

public class JdbcPanoptaDataService implements PanoptaDataService {

    private final DataSource dataSource;
    private final Config config;

    @Inject
    public JdbcPanoptaDataService(DataSource dataSource, Config config) {
        this.dataSource = dataSource;
        this.config = config;
    }

    @Override
    public void createOrUpdatePanoptaCustomer(String shopperId, String customerKey) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        createOrUpdatePanoptaCustomerFromKey(partnerCustomerKey, customerKey);
    }

    @Override
    public void createOrUpdatePanoptaCustomerFromKey(String partnerCustomerKey, String customerKey) {
        Sql.with(dataSource)
           .exec("INSERT INTO panopta_customer (partner_customer_key, customer_key) values (?,?) " +
                         "ON CONFLICT (partner_customer_key) DO UPDATE SET customer_key = ?, created = now_utc(), " +
                         "destroyed = 'infinity' ",
                 null, partnerCustomerKey, customerKey, customerKey);
    }

    private String getPartnerCustomerKey(String shopperId) {
        return config.get("panopta.api.partner.customer.key.prefix") + shopperId;
    }

    @Override
    public void createPanoptaServer(UUID vmId, String shopperId, String templateId, PanoptaServer panoptaServer) {
        Sql.with(dataSource)
           .exec("INSERT INTO panopta_server (partner_customer_key, vm_id, server_id, server_key, template_id) " +
                         "values (?,?,?,?,?) ",
                 null, getPartnerCustomerKey(shopperId), vmId, panoptaServer.serverId,
                 panoptaServer.serverKey, templateId);
    }

    @Override
    public void insertPanoptaServerFromKey(UUID vmId, String partnerCustomerKey, long serverId, String serverKey, String templateId) {
        Sql.with(dataSource)
                .exec("INSERT INTO panopta_server (partner_customer_key, vm_id, server_id, server_key, template_id) " +
                                "values (?,?,?,?,?)",
                        null, partnerCustomerKey, vmId, serverId, serverKey, templateId);
    }

    @Override
    public void setPanoptaServerDestroyed(UUID vmId) {
        Sql.with(dataSource)
           .exec("UPDATE panopta_server SET destroyed = now_utc() WHERE vm_id = ?", null, vmId);
    }

    @Override
    public void setPanoptaServerActive(UUID vmId) {
        Sql.with(dataSource)
           .exec("UPDATE panopta_server SET destroyed = 'infinity' WHERE vm_id = ?", null, vmId);
    }

    @Override
    public void deletePanoptaServer(UUID vmId) {
        Sql.with(dataSource)
           .exec("DELETE FROM panopta_server WHERE vm_id = ?", null, vmId);
    }

    @Override
    public void setAllPanoptaServersOfCustomerDestroyed(String shopperId) {
        Sql.with(dataSource)
                .exec("UPDATE panopta_server SET destroyed = now_utc() WHERE " +
                                "partner_customer_key = ? " +
                                "and destroyed = 'infinity'",
                        null, getPartnerCustomerKey(shopperId));
    }

    @Override
    public boolean checkAndSetPanoptaCustomerDestroyed(String shopperId) {
        if(noActivePanoptaServers(shopperId)) {
            Sql.with(dataSource)
               .exec("UPDATE panopta_customer SET destroyed = now_utc() WHERE partner_customer_key = ? ",
                     null, getPartnerCustomerKey(shopperId));
            return true;
        }
        return false;
    }

    private boolean noActivePanoptaServers(String shopperId) {
        return getPanoptaServerDetailsList(shopperId).size() == 0;
    }

    @Override
    public List<PanoptaServerDetails> getPanoptaServerDetailsList(String shopperId) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        String findActivePanoptaServersQuery = "SELECT partner_customer_key, vm_id, server_id, server_key, created, destroyed" +
                " FROM panopta_server WHERE partner_customer_key = ? AND destroyed = 'infinity' ";
        return Sql.with(dataSource).exec(findActivePanoptaServersQuery, Sql.listOf(this::mapPanoptaServerDetails), partnerCustomerKey);
    }

    @Override
    public PanoptaCustomerDetails getPanoptaCustomerDetails(String shopperId) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
        return Sql.with(dataSource).exec(
                "SELECT partner_customer_key, customer_key, created, destroyed " +
                        " FROM panopta_customer " +
                        " WHERE partner_customer_key = ?  and destroyed = 'infinity' ",
                Sql.nextOrNull(this::mapPanoptaCustomerDetails), partnerCustomerKey);
    }

    private PanoptaCustomerDetails mapPanoptaCustomerDetails(ResultSet resultSet) throws SQLException {
        PanoptaCustomerDetails panoptaCustomer = new PanoptaCustomerDetails();
        panoptaCustomer.setPartnerCustomerKey(resultSet.getString("partner_customer_key"));
        panoptaCustomer.setCustomerKey(resultSet.getString("customer_key"));
        panoptaCustomer.setCreated(resultSet.getTimestamp("created", TimestampUtils.utcCalendar).toInstant());
        panoptaCustomer.setDestroyed(resultSet.getTimestamp("destroyed", TimestampUtils.utcCalendar).toInstant());
        return panoptaCustomer;
    }

    @Override
    public PanoptaServerDetails getPanoptaServerDetails(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT partner_customer_key, vm_Id, server_id, server_key, created, destroyed " +
                        " FROM panopta_server " +
                        " WHERE vm_id = ? and destroyed = 'infinity' ",
                Sql.nextOrNull(this::mapPanoptaServerDetails), vmId);
    }

    private PanoptaServerDetails mapPanoptaServerDetails(ResultSet resultSet) throws SQLException {
        PanoptaServerDetails panoptaServer = new PanoptaServerDetails();
        panoptaServer.setPartnerCustomerKey(resultSet.getString("partner_customer_key"));
        panoptaServer.setVmId(UUID.fromString(resultSet.getString("vm_id")));
        panoptaServer.setServerId(resultSet.getLong("server_id"));
        panoptaServer.setServerKey(resultSet.getString("server_key"));
        panoptaServer.setCreated(resultSet.getTimestamp("created", TimestampUtils.utcCalendar).toInstant());
        panoptaServer.setDestroyed(resultSet.getTimestamp("destroyed", TimestampUtils.utcCalendar).toInstant());
        return panoptaServer;
    }

    @Override
    public PanoptaDetail getPanoptaDetails(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT pc.partner_customer_key, pc.customer_key, ps.vm_id, ps.server_id, ps.server_key, " +
                        " ps.created, ps.destroyed, ps.template_id " +
                        " FROM panopta_customer pc " +
                        " JOIN panopta_server ps USING (partner_customer_key) " +
                        " WHERE ps.vm_id = ?  AND ps.destroyed = 'infinity' ",
                Sql.nextOrNull(PanoptaDetailMapper::mapPanoptaDetails), vmId);
    }

    @Override
    public UUID getVmId(String serverKey) {
        return Sql.with(dataSource).exec("SELECT vm_id FROM panopta_server WHERE server_key = ? " +
                        "AND destroyed = 'infinity';",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("vm_id"))), serverKey);
    }

    @Override
    public List<String> getPanoptaActiveAdditionalFqdns(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT paf.fqdn " +
                        "FROM panopta_additional_fqdns paf " +
                        "JOIN panopta_server ps USING (id) " +
                        "WHERE ps.vm_id = ? AND ps.destroyed = 'infinity' " +
                        "AND paf.valid_until = 'infinity';",
                Sql.listOf(rs -> rs.getString("fqdn")), vmId);
    }

    @Override
    public void addPanoptaAdditionalFqdn(String fqdn, long panoptaServerId) {
        Sql.with(dataSource)
                .exec("INSERT INTO panopta_additional_fqdns (id, fqdn) " +
                                "SELECT id, ? " +
                                "FROM panopta_server ps " +
                                "WHERE ps.server_id = ? AND ps.destroyed = 'infinity';",
                        null, fqdn,
                        panoptaServerId);
    }

    @Override
    public boolean activeAdditionalFqdnExistsForServer(String fqdn, long panoptaServerId) {
        return Sql.with(dataSource).exec(
            "SELECT Count(*) " +
                    "FROM panopta_additional_fqdns paf " +
                    "JOIN panopta_server ps USING (id) " +
                    "WHERE ps.server_id = ? AND ps.destroyed = 'infinity' " +
                    "AND paf.fqdn = ? AND paf.valid_until = 'infinity';", this::mapFqdnExists, panoptaServerId, fqdn);
    }

    private boolean mapFqdnExists(ResultSet rs) throws SQLException {
        return rs.next() && rs.getLong("count") > 0;
    }

    @Override
    public void deletePanoptaAdditionalFqdn(String fqdn, long panoptaServerId) {
        Sql.with(dataSource)
                .exec("UPDATE panopta_additional_fqdns paf " +
                                "SET valid_until = now_utc() " +
                                "FROM panopta_server ps " +
                                "WHERE ps.server_id = ? AND ps.destroyed = 'infinity' " +
                                "AND paf.fqdn = ? AND paf.valid_until = 'infinity';",
                        null, panoptaServerId,
                        fqdn);
    }

    @Override
    public void deleteVirtualMachineAdditionalFqdns(UUID vmId) {
        Sql.with(dataSource)
                .exec("UPDATE panopta_additional_fqdns SET valid_until = now_utc() " +
                                "WHERE additional_fqdn_id in " +
                                "(SELECT paf.additional_fqdn_id FROM panopta_additional_fqdns paf " +
                                "JOIN panopta_server ps ON paf.id = ps.id " +
                                "WHERE ps.vm_id = ? AND ps.destroyed = 'infinity');",
                        null, vmId);
    }

    @Override
    public Map<String, Instant> getPanoptaAdditionalFqdnWithValidOn(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT paf.fqdn, paf.valid_on " +
                        "FROM panopta_additional_fqdns paf " +
                        "JOIN panopta_server ps USING (id) " +
                        "WHERE ps.vm_id = ? AND ps.destroyed = 'infinity' " +
                        "AND paf.valid_until = 'infinity';",
                Sql.mapOf(rs -> (rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant()),
                          rs -> rs.getString("fqdn").toLowerCase()), vmId);
    }
}
