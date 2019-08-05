package com.godaddy.vps4.panopta.jdbc;

import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;

public class JdbcPanoptaDataService implements PanoptaDataService {

    private final String tableName = "panopta_detail";
    private final DataSource dataSource;

    @Inject
    public JdbcPanoptaDataService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createPanoptaDetails(UUID vmId, PanoptaCustomer panoptaCustomer, PanoptaServer panoptaServer) {
        Sql.with(dataSource)
           .exec("INSERT INTO " + tableName + " (vm_id, partner_customer_key, customer_key, server_id, server_key) values (?,?,?,?,?) ",
                 null, vmId, panoptaCustomer.partnerCustomerKey, panoptaCustomer.customerKey, panoptaServer.serverId, panoptaServer.serverKey);
    }

    @Override
    public void setServerDestroyedInPanopta(UUID vmId) {
        Sql.with(dataSource)
           .exec("UPDATE " + tableName + " SET destroyed = now_utc() WHERE vm_id = ? ", null, vmId);

    }

    @Override
    public PanoptaDetail getPanoptaDetails(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM " + tableName + " WHERE vm_id = ? ",
                Sql.nextOrNull(PanoptaDetailMapper::mapPanoptaDetails), vmId);
    }
}
