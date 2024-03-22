// package com.godaddy.vps4.phase2.prodMeta;

// import javax.sql.DataSource;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.BeforeClass;
// import org.junit.Test;
// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertNotNull;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;

// import java.time.Instant;
// import java.util.ArrayList;
// import java.util.EnumMap;
// import java.util.List;
// import java.util.UUID;

// import com.godaddy.vps4.prodMeta.ProdMetaService;
// import com.godaddy.vps4.prodMeta.jdbc.JdbcProdMetaService;
// import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
// import com.godaddy.vps4.jdbc.DatabaseModule;
// import com.godaddy.vps4.prodMeta.model.ProdMeta;
// import com.godaddy.vps4.vm.DataCenter;
// import com.godaddy.vps4.vm.DataCenterService;
// import com.google.inject.AbstractModule;
// import com.google.inject.Guice;
// import com.google.inject.Injector;

// public class JdbcProdMetaServiceTest {
//     static private Injector injectorForDS;
//     private Injector injector;
//     static private DataSource dataSource;
//     static private DataCenterService dataCenterService;
//     List<ProdMeta> prodMetaList;

//     @BeforeClass
//     public static void setUpInternalInjector() {
//         injectorForDS = Guice.createInjector(new DatabaseModule());
//         dataSource = injectorForDS.getInstance(DataSource.class);
//         dataCenterService = mock(DataCenterService.class);
//         when(dataCenterService.getDataCenter(1)).thenReturn(new DataCenter(1, "test"));
//     }

//     @Before
//     public void setUp() {
//         prodMetaList = new ArrayList<>();
//         injector = Guice.createInjector(new AbstractModule() {
//             @Override
//             protected void configure() {
//                 bind(DataSource.class).toInstance(dataSource);
//                 bind(ProdMetaService.class).to(JdbcProdMetaService.class);
//                 bind(DataCenterService.class).toInstance(dataCenterService);
//             }
//         });
//     }

//     @After
//     public void tearDown() {
//         for (ProdMeta prodMeta : prodMetaList) {
//             injector.getInstance(ProdMetaService.class).deleteProdMeta(prodMeta.entitlementId);
//         }
//     }

//     private ProdMeta insertProdMeta(UUID entitlementId) {
//         injector.getInstance(ProdMetaService.class).insertProdMeta(entitlementId);
//         ProdMeta prodMeta = injector.getInstance(ProdMetaService.class).getProdMeta(entitlementId);
//         assertNotNull(prodMeta);

//         return prodMeta;
//     }

//     @Test
//     public void testInsertProdMeta() {
//         UUID entitlementId = UUID.randomUUID();

//         ProdMeta prodMeta = insertProdMeta(entitlementId);

//         assertEquals(entitlementId, prodMeta.entitlementId);
//     }

//     @Test
//     public void testGetProdMeta() {
//         UUID entitlementId1 = UUID.randomUUID();
//         ProdMeta prodMeta1 = insertProdMeta(entitlementId1);
//         UUID entitlementId2 = UUID.randomUUID();
//         ProdMeta prodMeta2 = insertProdMeta(entitlementId2);
//         UUID entitlementId3 = UUID.randomUUID();
//         ProdMeta prodMeta3 = insertProdMeta(entitlementId3);
//         ProdMetaService prodMetaService = injector.getInstance(ProdMetaService.class);
        
//         ProdMeta prodMetaActual2 = prodMetaService.getProdMeta(entitlementId2);
//         assertNotNull(prodMetaActual2);
//         assertEquals(prodMetaActual2.entitlementId, prodMeta2.entitlementId);

//         ProdMeta prodMetaActual3 = prodMetaService.getProdMeta(entitlementId3);
//         assertNotNull(prodMetaActual3);
//         assertEquals(prodMetaActual3.entitlementId, prodMeta3.entitlementId);

//         ProdMeta prodMetaActual1 = prodMetaService.getProdMeta(entitlementId1);
//         assertNotNull(prodMetaActual1);
//         assertEquals(prodMetaActual1.entitlementId, prodMeta1.entitlementId);
//     }

//     @Test
//     public void testUpdateProdMeta() {
//         UUID entitlementId = UUID.randomUUID();
//         ProdMeta prodMeta = insertProdMeta(entitlementId);

//         prodMeta.dataCenter = 1;
//         prodMeta.productId = UUID.randomUUID();
//         prodMeta.provisionDate = Instant.now();
//         prodMeta.fullyManagedEmailSent = true;
//         prodMeta.purchasedAt = Instant.now();
//         prodMeta.releasedAt = Instant.now();
//         prodMeta.relayCount = 99;

//         EnumMap<ProductMetaField, Object> prodMetaMap = new EnumMap<>(ProductMetaField.class);
//         prodMetaMap.put(ProductMetaField.DATA_CENTER, prodMeta.dataCenter);
//         prodMetaMap.put(ProductMetaField.PRODUCT_ID, prodMeta.productId);
//         prodMetaMap.put(ProductMetaField.PROVISION_DATE, prodMeta.provisionDate);
//         prodMetaMap.put(ProductMetaField.FULLY_MANAGED_EMAIL_SENT, prodMeta.fullyManagedEmailSent);
//         prodMetaMap.put(ProductMetaField.PURCHASED_AT, prodMeta.purchasedAt);
//         prodMetaMap.put(ProductMetaField.RELEASED_AT, prodMeta.releasedAt);
//         prodMetaMap.put(ProductMetaField.RELAY_COUNT, prodMeta.relayCount);

//         injector.getInstance(ProdMetaService.class).updateProdMeta(entitlementId, prodMetaMap);
//         ProdMeta updatedProdMeta = injector.getInstance(ProdMetaService.class).getProdMeta(entitlementId);
//         assertNotNull(updatedProdMeta);
//         assertEquals(prodMeta.dataCenter, updatedProdMeta.dataCenter);
//         assertEquals(prodMeta.productId, updatedProdMeta.productId);
//         assertEquals(prodMeta.provisionDate, updatedProdMeta.provisionDate);
//         assertEquals(prodMeta.fullyManagedEmailSent, updatedProdMeta.fullyManagedEmailSent);
//         assertEquals(prodMeta.purchasedAt, updatedProdMeta.purchasedAt);
//         assertEquals(prodMeta.releasedAt, updatedProdMeta.releasedAt);
//         assertEquals(prodMeta.relayCount, updatedProdMeta.relayCount);
//     }

//     @Test
//     public void testGetProdMetaByVmId() {
//         UUID vmId = UUID.randomUUID();
//         UUID entitlementId = UUID.randomUUID();
//         ProdMeta prodMeta = insertProdMeta(entitlementId);

//         prodMeta.productId = vmId;
//         EnumMap<ProductMetaField, Object> prodMetaMap = new EnumMap<>(ProductMetaField.class);
//         prodMetaMap.put(ProductMetaField.PRODUCT_ID, prodMeta.productId);
//         injector.getInstance(ProdMetaService.class).updateProdMeta(entitlementId, prodMetaMap);

//         ProdMeta prodMetaByVmId = injector.getInstance(ProdMetaService.class).getProdMetaByVmId(vmId);
//         assertNotNull(prodMetaByVmId);
//         assertEquals(entitlementId, prodMetaByVmId.entitlementId);
//         assertEquals(prodMeta.productId, vmId);
//     }

//     @Test
//     public void testDeleteProdMeta() {
//         UUID entitlementId1 = UUID.randomUUID();
//         insertProdMeta(entitlementId1);
//         UUID entitlementId2 = UUID.randomUUID();
//         insertProdMeta(entitlementId2);

//         injector.getInstance(ProdMetaService.class).deleteProdMeta(entitlementId1);
//         ProdMeta deletedProdMeta = injector.getInstance(ProdMetaService.class).getProdMeta(entitlementId1);
//         assertEquals(null, deletedProdMeta);
//         ProdMeta prodMetaActual2 = injector.getInstance(ProdMetaService.class).getProdMeta(entitlementId2);
//         assertNotNull(prodMetaActual2);
//     }
// }
