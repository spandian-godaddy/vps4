package com.godaddy.vps4.credit.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.Vps4CreditService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.AccountStatus;

public class JdbcVps4CreditService implements Vps4CreditService {

    private final DataSource dataSource;

    @Inject
    public JdbcVps4CreditService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createVirtualMachineCredit(UUID orionGuid, String operatingSystem, String controlPanel, int tier, int managedLevel, String shopperId) {
        Sql.with(dataSource).exec("SELECT * FROM credit_create(?,?,?,?,?,?)",
                null, orionGuid, operatingSystem, tier,
                controlPanel, managedLevel, shopperId);
    }

    @Override
    public VirtualMachineCredit getVirtualMachineCredit(UUID orionGuid) {
        return Sql.with(dataSource).exec("SELECT * FROM credit WHERE orion_guid = ?",
                Sql.nextOrNull(this::mapVirtualMachineCredit), orionGuid);
    }

    private VirtualMachineCredit mapVirtualMachineCredit(ResultSet rs) throws SQLException {
        Timestamp provisionDate = rs.getTimestamp("provision_date");

        return new VirtualMachineCredit(java.util.UUID.fromString(rs.getString("orion_guid")), rs.getInt("tier"),
                rs.getInt("managed_level"), rs.getInt("monitoring"), rs.getString("operating_system"), rs.getString("control_panel"),
                rs.getTimestamp("create_date").toInstant(), provisionDate != null ? provisionDate.toInstant() : null,
                rs.getString("shopper_id"), AccountStatus.ACTIVE);
    }

    @Override
    public List<VirtualMachineCredit> getVirtualMachineCredits(String shopperId) {
        return Sql.with(dataSource).exec(
                "SELECT * from credit WHERE shopper_id = ? AND provision_date IS NULL",
                Sql.listOf(this::mapVirtualMachineCredit), shopperId);
    }

    @Override
    public void createCreditIfNoneExists(Vps4User vps4User) {
        Sql.with(dataSource).exec("SELECT * FROM auto_create_credit(?, ?, ?, ?, ?)", null, vps4User.getId(), 10, "linux", "cpanel",
                1);
    }

    @Override
    public void claimVirtualMachineCredit(UUID orionGuid, int dataCenterId) {
        Sql.with(dataSource).exec("UPDATE credit SET provision_date = NOW() WHERE orion_guid = ?",
                null, orionGuid);
    }

    @Override
    public void unclaimVirtualMachineCredit(UUID orionGuid) {
        Sql.with(dataSource).exec("UPDATE credit SET provision_date = NULL WHERE orion_guid = ?",
                null, orionGuid);
    }

}
