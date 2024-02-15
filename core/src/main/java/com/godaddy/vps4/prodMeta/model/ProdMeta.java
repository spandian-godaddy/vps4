package com.godaddy.vps4.prodMeta.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;

import java.time.Instant;

public class ProdMeta {
    public UUID entitlementId;
    public int dataCenter;
    public UUID productId;
    public Instant provisionDate;
    public boolean fullyManagedEmailSent;
    public Instant purchasedAt;
    public Instant releasedAt;
    public int relayCount;

    public ProdMeta() {}

    public ProdMeta(Map<ProductMetaField, String> productMeta, DataCenterService dataCenterService) {
        if (isKeyParseable(productMeta, ProductMetaField.DATA_CENTER)) {
            this.dataCenter = Integer.parseInt(productMeta.get(ProductMetaField.DATA_CENTER));
        }
        if (isKeyParseable(productMeta, ProductMetaField.PRODUCT_ID)) {
            this.productId = UUID.fromString(productMeta.get(ProductMetaField.PRODUCT_ID));
        }
        if (isKeyParseable(productMeta, ProductMetaField.PROVISION_DATE)) {
            this.provisionDate = Instant.parse(productMeta.get(ProductMetaField.PROVISION_DATE));
        }
        if (isKeyParseable(productMeta, ProductMetaField.FULLY_MANAGED_EMAIL_SENT)) {
            this.fullyManagedEmailSent = Boolean.parseBoolean(productMeta.get(ProductMetaField.FULLY_MANAGED_EMAIL_SENT));
        }
        if (isKeyParseable(productMeta, ProductMetaField.PURCHASED_AT)) {
            this.purchasedAt = Instant.parse(productMeta.get(ProductMetaField.PURCHASED_AT));
        }
        if (isKeyParseable(productMeta, ProductMetaField.RELEASED_AT)) {
            this.releasedAt = Instant.parse(productMeta.get(ProductMetaField.RELEASED_AT));
        }
        if (isKeyParseable(productMeta, ProductMetaField.RELAY_COUNT)) {
            this.relayCount = Integer.parseInt(productMeta.get(ProductMetaField.RELAY_COUNT));
        }
    }

    private boolean isKeyParseable(Map<ProductMetaField, String> productMeta, ProductMetaField field) {  
        return productMeta.containsKey(field) && !StringUtils.isEmpty(productMeta.get(field));  
    }

    @JsonIgnore
    public Map<ProductMetaField, Object> getProdMetaMap() {
        Map<ProductMetaField, Object> fields = new EnumMap<>(ProductMetaField.class);
        if(dataCenter != 0) fields.put(ProductMetaField.DATA_CENTER, dataCenter);
        if(productId != null) fields.put(ProductMetaField.PRODUCT_ID, productId);
        if(provisionDate != null) fields.put(ProductMetaField.PROVISION_DATE, provisionDate);
        fields.put(ProductMetaField.FULLY_MANAGED_EMAIL_SENT, fullyManagedEmailSent);
        if(purchasedAt != null) fields.put(ProductMetaField.PURCHASED_AT, purchasedAt);
        if(releasedAt != null) fields.put(ProductMetaField.RELEASED_AT, releasedAt);
        if(relayCount != 0) fields.put(ProductMetaField.RELAY_COUNT, relayCount);

        return fields;
    }
}
