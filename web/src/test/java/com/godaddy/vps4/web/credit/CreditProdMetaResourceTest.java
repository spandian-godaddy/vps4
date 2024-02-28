package com.godaddy.vps4.web.credit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.prodMeta.ProdMetaService;
import com.godaddy.vps4.prodMeta.ExternalDc.CrossDcProdMetaClientService;
import com.godaddy.vps4.prodMeta.model.ProdMeta;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;

public class CreditProdMetaResourceTest {
    private CreditProdMetaResource creditProdMetaResource;
    private ProdMetaService prodMetaService;
    private CreditService creditService;
    private DataCenterService dataCenterService;
    private CrossDcProdMetaClientService crossDcProdMetaClientService;

    @Before
    public void setUp() {
        prodMetaService = mock(ProdMetaService.class);
        creditService = mock(CreditService.class);
        dataCenterService = mock(DataCenterService.class);
        crossDcProdMetaClientService = mock(CrossDcProdMetaClientService.class);
        creditProdMetaResource = new CreditProdMetaResource(prodMetaService, creditService, dataCenterService, crossDcProdMetaClientService);
    }

    @Test
    public void testGetProdMeta() {
        UUID entitlementId = UUID.randomUUID();
        ProdMeta expectedProdMeta = new ProdMeta();
        expectedProdMeta.entitlementId = UUID.randomUUID();
        expectedProdMeta.productId = UUID.randomUUID();
        when(prodMetaService.getProdMeta(entitlementId)).thenReturn(expectedProdMeta);

        ProdMeta actualProdMeta = creditProdMetaResource.getProdMeta(entitlementId);

        assertEquals(expectedProdMeta, actualProdMeta);
    }

    @Test
    public void testGetProdMetaFromHfs() {
        UUID entitlementId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Map<ProductMetaField, String> prodMetaMap = new EnumMap<ProductMetaField, String>(ProductMetaField.class) {
            {
                put(ProductMetaField.DATA_CENTER, "1");
                put(ProductMetaField.PRODUCT_ID, productId.toString());
                put(ProductMetaField.PROVISION_DATE, "2020-01-01T00:00:00Z");
                put(ProductMetaField.FULLY_MANAGED_EMAIL_SENT, "true");
                put(ProductMetaField.PURCHASED_AT, "2021-01-01T00:00:00Z");
                put(ProductMetaField.RELEASED_AT, "2022-01-01T00:00:00Z");
                put(ProductMetaField.RELAY_COUNT, "1000");
            }
        };
        when(prodMetaService.getProdMeta(entitlementId)).thenReturn(null);
        when(creditService.getProductMeta(entitlementId)).thenReturn(prodMetaMap);
        DataCenter dataCenter = new DataCenter();
        dataCenter.dataCenterId = 1;
        dataCenter.dataCenterName = "Test Data Center";
        when(dataCenterService.getDataCenter(1)).thenReturn(dataCenter);

        ProdMeta actualProdMeta = creditProdMetaResource.getProdMeta(entitlementId);

        assertEquals(1, actualProdMeta.dataCenter);
        assertEquals(productId, actualProdMeta.productId);
        assertEquals("2020-01-01T00:00:00Z", actualProdMeta.provisionDate.toString());
        assertEquals(true, actualProdMeta.fullyManagedEmailSent);
        assertEquals("2021-01-01T00:00:00Z", actualProdMeta.purchasedAt.toString());
        assertEquals("2022-01-01T00:00:00Z", actualProdMeta.releasedAt.toString());
        assertEquals(1000, actualProdMeta.relayCount);
        verify(prodMetaService, times(1)).insertProdMeta(entitlementId);
        verify(prodMetaService, times(1)).updateProdMeta(eq(entitlementId), anyMap());
    }

    @Test
    public void testSetProdMeta() {
        UUID entitlementId = UUID.randomUUID();
        ProdMeta expectedProdMeta = new ProdMeta();
        expectedProdMeta.entitlementId = UUID.randomUUID();
        expectedProdMeta.productId = UUID.randomUUID();
        when(prodMetaService.getProdMeta(entitlementId)).thenReturn(expectedProdMeta);

        ProdMeta actualProdMeta = creditProdMetaResource.setProdMeta(entitlementId);

        assertEquals(expectedProdMeta, actualProdMeta);
        verify(prodMetaService, times(1)).insertProdMeta(entitlementId);
    }

    @Test
    public void testUpdateProdMeta() {
        UUID entitlementId = UUID.randomUUID();
        ProdMeta prodMeta = new ProdMeta();
        prodMeta.productId = UUID.randomUUID();

        creditProdMetaResource.updateProdMeta(entitlementId, prodMeta);

        verify(prodMetaService, times(1))
                .updateProdMeta(entitlementId, prodMeta.getProdMetaMap());
    }

    @Test
    public void testUpdateProdMetaInsertFirst() {
        UUID entitlementId = UUID.randomUUID();
        ProdMeta prodMeta = new ProdMeta();
        prodMeta.productId = UUID.randomUUID();
        when(prodMetaService.getProdMeta(entitlementId)).thenReturn(null);

        creditProdMetaResource.updateProdMeta(entitlementId, prodMeta);

        verify(prodMetaService, times(1)).insertProdMeta(entitlementId);
        verify(prodMetaService, times(1))
                .updateProdMeta(entitlementId, prodMeta.getProdMetaMap());
    }

    @Test
    public void testDeleteProdMeta() {
        UUID entitlementId = UUID.randomUUID();

        creditProdMetaResource.deleteProdMeta(entitlementId);

        verify(prodMetaService, times(1)).deleteProdMeta(entitlementId);
    }

    @Test
    public void testGetProdMetaCrossDc() {
        UUID entitlementId = UUID.randomUUID();
        ProdMeta expectedProdMeta = new ProdMeta();
        expectedProdMeta.entitlementId = entitlementId;
        when(crossDcProdMetaClientService.getProdMeta(entitlementId)).thenReturn(expectedProdMeta);

        ProdMeta actualProdMeta = creditProdMetaResource.getProdMetaCrossDc(entitlementId);

        assertEquals(expectedProdMeta, actualProdMeta);
        verify(crossDcProdMetaClientService, times(1)).getProdMeta(entitlementId);
    }
}