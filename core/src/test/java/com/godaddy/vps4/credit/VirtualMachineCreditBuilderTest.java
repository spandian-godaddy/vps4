package com.godaddy.vps4.credit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.credit.VirtualMachineCredit.EntitlementBuilder;
import com.godaddy.vps4.entitlement.models.Product;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;

public class VirtualMachineCreditBuilderTest {
    final private DataCenterService dataCenterService = mock(DataCenterService.class);

    private Product entitlementProduct;
    private final int tier = 10;
    private final int managedLevel = 1;
    private final Boolean monitoring = true;
    private final String operatingSystem = "linux";
    private final String controlPanel = "cpanel";

    private Map<String, String> productMeta;
    private final int dcId = 1;
    private final DataCenter dc = new DataCenter(dcId, "foobar");
    private final Instant provisionDate = Instant.now();
    private final Boolean fullyManagedEmailSent = Boolean.FALSE;
    private final UUID productId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        when(dataCenterService.getDataCenter(eq(dcId))).thenReturn(dc);
        entitlementProduct = new Product();
        entitlementProduct.planTier = tier;
        entitlementProduct.managedLevel = managedLevel;
        entitlementProduct.monitoring = monitoring;
        entitlementProduct.operatingSystem = operatingSystem;
        entitlementProduct.controlPanelType = controlPanel;
        entitlementProduct.cdnWaf = 2;
        entitlementProduct.mssql = "web";

        productMeta = new HashMap<>();
        productMeta.put(ProductMetaField.DATA_CENTER.toString(), String.valueOf(dcId));
        productMeta.put(ProductMetaField.PROVISION_DATE.toString(), provisionDate.toString());
        productMeta.put(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString(), fullyManagedEmailSent.toString());
        productMeta.put(ProductMetaField.PRODUCT_ID.toString(), productId.toString());
    }

    @Test
    public void withEntitlementId() {
        UUID entitlementId = UUID.randomUUID();
        VirtualMachineCredit credit = new EntitlementBuilder()
            .withEntitlementId(entitlementId).build();
        assertEquals(entitlementId, credit.getEntitlementId());
    }

    @Test
    public void withEntitlementProduct() {
        VirtualMachineCredit credit = new EntitlementBuilder()
            .withEntitlementProduct(entitlementProduct).build();
        assertEquals(entitlementProduct.planTier.intValue(), credit.getTier());
        assertEquals(entitlementProduct.managedLevel.intValue(), credit.getManagedLevel());
        assertEquals(entitlementProduct.monitoring, credit.hasMonitoring());
        assertEquals(entitlementProduct.operatingSystem, credit.getOperatingSystem());
        assertEquals(entitlementProduct.controlPanelType, credit.getControlPanel());
        assertEquals(entitlementProduct.cdnWaf.intValue(), credit.getCdnWaf());
        assertEquals(entitlementProduct.mssql, credit.getMssql());
    }

    @Test
    public void withProductMeta() {
        VirtualMachineCredit credit = new EntitlementBuilder()
                .withProductMeta(productMeta).build();
        assertEquals(dc.dataCenterId, credit.prodMeta.dataCenter);
        assertEquals(provisionDate, credit.getProvisionDate());
        assertEquals(fullyManagedEmailSent, credit.isFullyManagedEmailSent());
        assertEquals(productId, credit.getProductId());
    }

    @Test
    public void withShopperID() {
        String shopperId = "testShopper";
        VirtualMachineCredit credit = new EntitlementBuilder()
            .withShopperID(shopperId).build();
        assertEquals(shopperId, credit.getShopperId());
    }

    @Test
    public void withoutShopperID() {
        VirtualMachineCredit credit = new EntitlementBuilder().build();
        assertNull(credit.getShopperId());
    }

    @Test
    public void withCustomerID() {
        UUID customerId = UUID.randomUUID();
        VirtualMachineCredit credit = new EntitlementBuilder()
            .withCustomerID(customerId).build();
        assertEquals(customerId, credit.getCustomerId());
    }

    @Test
    public void withResellerID() {
        String resellerId = "123";
        VirtualMachineCredit credit = new EntitlementBuilder()
                .withResellerID(resellerId).build();
        assertEquals(resellerId, credit.getResellerId());
    }

    @Test
    public void withoutResellerID() {
        VirtualMachineCredit credit = new EntitlementBuilder().build();
        assertNull(credit.getResellerId());
    }

    @Test
    public void havingAccountStatus() {
        AccountStatus status = AccountStatus.ACTIVE;
        VirtualMachineCredit credit = new EntitlementBuilder()
            .withAccountStatus(status.toString()).build();
        assertEquals(AccountStatus.ACTIVE, credit.getAccountStatus());
    }

    @Test
    public void withoutAccountStatus() {
        VirtualMachineCredit credit = new EntitlementBuilder().build();
        assertNull(credit.getAccountStatus());
    }

    @Test
    public void withExpireDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date expireDate = new Date();
        try {
            VirtualMachineCredit credit = new EntitlementBuilder()
                .withExpireDate(sdf.format(expireDate).toString()).build();
            assertEquals(sdf.parse(sdf.format(expireDate)).toInstant(), credit.getExpireDate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
