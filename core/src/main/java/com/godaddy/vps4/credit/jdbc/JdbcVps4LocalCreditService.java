package com.godaddy.vps4.credit.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.Vps4LocalCreditService;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.AccountStatus;
import com.google.inject.Inject;

public class JdbcVps4LocalCreditService implements Vps4LocalCreditService{
    private final DataSource dataSource;

    @Inject
    public JdbcVps4LocalCreditService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void insertCredit(VirtualMachineCredit credit) {
        Sql.with(dataSource).exec(
            "INSERT INTO credit " +
                "(credit_id, tier, managed_level, operating_system, control_panel, provision_date, shopper_id, " +
                "monitoring, account_status, data_center, product_id, fully_managed_email_sent, reseller_id, pfid, " + 
                "purchased_at, customer_id, expire_date, mssql, cdn_waf) " + 
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", 
            null, 
            credit.entitlementData.entitlementId, 
            credit.entitlementData.tier,
            credit.entitlementData.managedLevel,
            credit.entitlementData.operatingSystem,
            credit.entitlementData.controlPanel,
            (credit.prodMeta.provisionDate == null) ? null : Timestamp.from(credit.prodMeta.provisionDate),
            credit.shopperId,
            credit.entitlementData.monitoring,
            (credit.entitlementData.accountStatus == null) ? null : credit.entitlementData.accountStatus.getAccountStatusId(),
            credit.prodMeta.dataCenter,
            credit.prodMeta.productId,
            credit.prodMeta.fullyManagedEmailSent,
            credit.resellerId,
            credit.entitlementData.pfid,
            (credit.prodMeta.purchasedAt == null) ? null : Timestamp.from(credit.prodMeta.purchasedAt),
            credit.entitlementData.customerId,
            (credit.entitlementData.expireDate == null) ? null : Timestamp.from(credit.entitlementData.expireDate),
            credit.entitlementData.mssql,
            credit.entitlementData.cdnWaf);
    }

    @Override
    public VirtualMachineCredit getCredit(UUID creditId) {
        return Sql.with(dataSource).exec(
            "SELECT * FROM credit WHERE credit_id = ?",
            Sql.nextOrNull(this::mapCredit),
            creditId);
    }

    @Override
    public VirtualMachineCredit getCreditByVmId(UUID vmId) {
        return Sql.with(dataSource).exec(
            "SELECT * FROM credit WHERE product_id = ?",
            Sql.nextOrNull(this::mapCredit),
            vmId);
    }

    @Override
    public List<VirtualMachineCredit> getCreditByShopperId(String shopperId) {
        return Sql.with(dataSource).exec(
            "SELECT * FROM credit WHERE shopper_id = ?",
            Sql.listOf(this::mapCredit),
            shopperId);
    }

    @Override
    public List<VirtualMachineCredit> getCreditByCustomerId(UUID customerId) {
        return Sql.with(dataSource).exec(
            "SELECT * FROM credit WHERE customer_id = ?",
            Sql.listOf(this::mapCredit),
            customerId);
    }

    @Override
    public void updateCredit(VirtualMachineCredit credit) {
        Sql.with(dataSource).exec(
            "UPDATE credit " +
                "SET tier=?, managed_level=?, operating_system=?, control_panel=?, provision_date=?, shopper_id=?, " +
                "monitoring=?, account_status=?, data_center=?, product_id=?, fully_managed_email_sent=?, " + 
                "reseller_id=?, pfid=?, purchased_at=?, customer_id=?, expire_date=?, mssql=?, cdn_waf=? " +
                "WHERE credit_id=?", 
            null, 
            credit.entitlementData.tier,
            credit.entitlementData.managedLevel,
            credit.entitlementData.operatingSystem,
            credit.entitlementData.controlPanel,
            (credit.prodMeta.provisionDate == null) ? null : Timestamp.from(credit.prodMeta.provisionDate),
            credit.shopperId,
            credit.entitlementData.monitoring,
            credit.entitlementData.accountStatus.getAccountStatusId(),
            credit.prodMeta.dataCenter,
            credit.prodMeta.productId,
            credit.prodMeta.fullyManagedEmailSent,
            credit.resellerId,
            credit.entitlementData.pfid,
            (credit.prodMeta.purchasedAt == null) ? null : Timestamp.from(credit.prodMeta.purchasedAt),
            credit.entitlementData.customerId,
            (credit.entitlementData.expireDate == null) ? null : Timestamp.from(credit.entitlementData.expireDate),
            credit.entitlementData.mssql,
            credit.entitlementData.cdnWaf,
            credit.entitlementData.entitlementId);
    }

    @Override
    public void deleteCredit(UUID creditId) {
        Sql.with(dataSource).exec("DELETE from credit where credit_id = ?", null, creditId);
    }

    private VirtualMachineCredit mapCredit(ResultSet rs) throws SQLException {
        VirtualMachineCredit credit = new VirtualMachineCredit();
        credit.entitlementData.entitlementId = UUID.fromString(rs.getString("credit_id"));
        credit.entitlementData.tier = rs.getInt("tier");
        credit.entitlementData.managedLevel = rs.getInt("managed_level");
        credit.entitlementData.operatingSystem = rs.getString("operating_system");
        credit.entitlementData.controlPanel = rs.getString("control_panel");
        credit.prodMeta.provisionDate = getTimeStampFromRS(rs, "provision_date");
        credit.shopperId = rs.getString("shopper_id");
        credit.entitlementData.monitoring = rs.getInt("monitoring");
        credit.entitlementData.accountStatus = AccountStatus.valueOf(rs.getInt("account_status"));
        credit.prodMeta.dataCenter = rs.getInt("data_center");
        credit.prodMeta.productId = getUuidFromRs(rs, "product_id");
        credit.prodMeta.fullyManagedEmailSent = rs.getBoolean("fully_managed_email_sent");
        credit.resellerId = rs.getString("reseller_id");
        credit.entitlementData.pfid = rs.getInt("pfid");
        credit.prodMeta.purchasedAt = getTimeStampFromRS(rs, "purchased_at");
        credit.entitlementData.customerId = getUuidFromRs(rs, "customer_id");
        credit.entitlementData.expireDate = getTimeStampFromRS(rs, "expire_date");
        credit.entitlementData.mssql = rs.getString("mssql");
        credit.entitlementData.cdnWaf = rs.getInt("cdn_waf");
        return credit;
    }

    private UUID getUuidFromRs(ResultSet rs, String columnName) throws SQLException {
        String uuidString = rs.getString(columnName);
        return uuidString == null ? null : UUID.fromString(uuidString);
    }

    private Instant getTimeStampFromRS(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName) == null ? null : 
            rs.getTimestamp(columnName, TimestampUtils.utcCalendar).toInstant();
    }
}
