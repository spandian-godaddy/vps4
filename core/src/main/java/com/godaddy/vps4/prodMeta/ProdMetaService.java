package com.godaddy.vps4.prodMeta;

import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.prodMeta.model.ProdMeta;

public interface ProdMetaService {
    void insertProdMeta(UUID entitlementId);
    ProdMeta getProdMeta(UUID entitlementId);
    ProdMeta getProdMetaByVmId(UUID vmId);
    void updateProdMeta(UUID entitlementId, Map<ProductMetaField, Object> paramsToUpdate);
    void deleteProdMeta(UUID entitlementId);
}
