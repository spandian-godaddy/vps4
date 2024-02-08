package com.godaddy.vps4.prodMeta.jdbc;

import java.sql.ResultSet;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import com.godaddy.hfs.jdbc.Sql;
import com.google.inject.Inject;
import com.godaddy.vps4.prodMeta.model.ProdMeta;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.prodMeta.ProdMetaService;

public class JdbcProdMetaService implements ProdMetaService {
    private final DataSource dataSource;
    private final DataCenterService dataCenterService;

    @Inject
    public JdbcProdMetaService(DataSource dataSource, DataCenterService dataCenterService) {
        this.dataSource = dataSource;
        this.dataCenterService = dataCenterService;
    }

    @Override
    public void insertProdMeta(UUID entitlementId) {
        Sql.with(dataSource).exec("INSERT INTO prod_meta (entitlement_id) VALUES (?)", null, entitlementId);
    }

    @Override
    public ProdMeta getProdMeta(UUID entitlementId) {
        return Sql.with(dataSource).exec(
            "SELECT entitlement_id, data_center, product_id, provision_date, " + 
            "fully_managed_email_sent, purchased_at, released_at, relay_count FROM prod_meta WHERE entitlement_id = ?",
            Sql.nextOrNull(this::mapProdMeta), entitlementId);
    }

    @Override
    public ProdMeta getProdMetaByVmId(UUID vmId) {
        return Sql.with(dataSource).exec(
            "SELECT entitlement_id, data_center, product_id, provision_date, " + 
            "fully_managed_email_sent, purchased_at, released_at, relay_count FROM prod_meta WHERE product_id = ?",
            Sql.nextOrNull(this::mapProdMeta), vmId);
    }

    private Instant getTimeStampFromRS(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName) == null ? null : 
            rs.getTimestamp(columnName, TimestampUtils.utcCalendar).toInstant();
    }

    private UUID getUuidFromRS(ResultSet rs, String columnName) throws SQLException {
        UUID uuid = null;
        String uuidString = rs.getString(columnName);

        if (uuidString != null) {
            uuid = UUID.fromString(uuidString);
        }

        return uuid;
    }

    private ProdMeta mapProdMeta(ResultSet rs) throws SQLException {
        ProdMeta prodMeta = new ProdMeta();
        prodMeta.entitlementId = UUID.fromString(rs.getString("entitlement_id"));
        prodMeta.dataCenter = dataCenterService.getDataCenter(rs.getInt("data_center"));
        prodMeta.productId = getUuidFromRS(rs, "product_id");
        prodMeta.provisionDate = getTimeStampFromRS(rs, "provision_date");
        prodMeta.fullyManagedEmailSent = rs.getBoolean("fully_managed_email_sent");
        prodMeta.purchasedAt = getTimeStampFromRS(rs, "purchased_at");
        prodMeta.releasedAt = getTimeStampFromRS(rs, "released_at");
        prodMeta.relayCount = rs.getInt("relay_count");
        return prodMeta;
    }

    @Override
    public void updateProdMeta(UUID entitlementId, Map<ProductMetaField, Object> paramsToUpdate) {
        if (!paramsToUpdate.isEmpty()) {
            ArrayList<Object> values = new ArrayList<>();
            StringBuilder nameSets = new StringBuilder();
            nameSets.append("UPDATE prod_meta vm SET ");
            for (Map.Entry<ProductMetaField, Object> pair : paramsToUpdate.entrySet()) {
                if (!values.isEmpty())
                    nameSets.append(", ");
                nameSets.append(pair.getKey());
                nameSets.append("=?");
                Object value = pair.getValue();
                if (value instanceof Instant)
                    value = LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC);
                values.add(value);
            }
            nameSets.append(" WHERE vm.entitlement_id = ?");
            values.add(entitlementId);
            Sql.with(dataSource).exec(nameSets.toString(), null, values.toArray());
        }
    }

    @Override
    public void deleteProdMeta(UUID entitlementId) {
        Sql.with(dataSource).exec("DELETE FROM prod_meta WHERE entitlement_id = ?", null, entitlementId);
    }
}
