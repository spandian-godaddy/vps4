package com.godaddy.vps4.panopta.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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
    public void createPanoptaCustomer(String shopperId, String customerKey) {
        String partnerCustomerKey = getPartnerCustomerKey(shopperId);
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
    public void createPanoptaServer(UUID vmId, String shopperId, PanoptaServer panoptaServer) {
        Sql.with(dataSource)
           .exec("INSERT INTO panopta_server (partner_customer_key, vm_id, server_id, server_key) " +
                         "values (?,?,?,?) ",
                 null, getPartnerCustomerKey(shopperId), vmId, panoptaServer.serverId,
                 panoptaServer.serverKey);
    }

    @Override
    public void setPanoptaServerDestroyed(UUID vmId) {
        Sql.with(dataSource)
           .exec("UPDATE panopta_server SET destroyed = now_utc() WHERE vm_id = ? ", null, vmId);
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
        return getActivePanoptaServers(shopperId).size() == 0;
    }

    @Override
    public List<PanoptaServerDetails> getActivePanoptaServers(String shopperId) {
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
                        " WHERE vm_id = ?  and destroyed = 'infinity' ",
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
                        " ps.created, ps.destroyed " +
                        " FROM panopta_customer pc " +
                        " JOIN panopta_server ps USING (partner_customer_key) " +
                        " WHERE ps.vm_id = ?  AND ps.destroyed = 'infinity' ",
                Sql.nextOrNull(PanoptaDetailMapper::mapPanoptaDetails), vmId);
    }

    @Override
    public UUID getVmId(String serverKey) {
        return Sql.with(dataSource).exec("SELECT vm_id FROM panopta_server WHERE server_key = ?;",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("vm_id"))), serverKey);
    }

}
