package com.godaddy.vps4.credit;

import static com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import static com.godaddy.vps4.credit.VirtualMachineCredit.Builder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;


public class BuilderTest {
    final private DataCenterService dataCenterService = mock(DataCenterService.class);

    private Map<String, String> planFeatures;
    private final int tier = 10;
    private final int managedLevel = 1;
    private final int monitoring = 1;
    private final String operatingSystem = "linux";
    private final String controlPanel = "cpanel";

    private Map<String, String> productMeta;
    private final int dcId = 1;
    private final DataCenter dc = new DataCenter(dcId, "foobar");
    private final Instant provisionDate = Instant.now();
    private final Boolean fullyManagedEmailSent = Boolean.FALSE;
    private final Boolean planChangePending = Boolean.TRUE;
    private final UUID productId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        when(dataCenterService.getDataCenter(eq(dcId))).thenReturn(dc);
        planFeatures = new HashMap<>();
        planFeatures.put(PlanFeatures.TIER.toString(), String.valueOf(tier));
        planFeatures.put(PlanFeatures.MANAGED_LEVEL.toString(), String.valueOf(managedLevel));
        planFeatures.put(PlanFeatures.MONITORING.toString(), String.valueOf(monitoring));
        planFeatures.put(PlanFeatures.OPERATINGSYSTEM.toString(), operatingSystem);
        planFeatures.put(PlanFeatures.CONTROL_PANEL_TYPE.toString(), controlPanel);

        productMeta = new HashMap<>();
        productMeta.put(ProductMetaField.DATA_CENTER.toString(), String.valueOf(dcId));
        productMeta.put(ProductMetaField.PROVISION_DATE.toString(), provisionDate.toString());
        productMeta.put(ProductMetaField.FULLY_MANAGED_EMAIL_SENT.toString(), fullyManagedEmailSent.toString());
        productMeta.put(ProductMetaField.PLAN_CHANGE_PENDING.toString(), planChangePending.toString());
        productMeta.put(ProductMetaField.PRODUCT_ID.toString(), productId.toString());
        productMeta.put(ProductMetaField.SUSPENDED.toString(), String.valueOf(false));
    }

    @Test
    public void withAccountGuid() {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withAccountGuid(orionGuid.toString()).build();
        assertEquals(orionGuid, credit.getEntitlementId());
    }

    @Test
    public void withoutAccountGuid() {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getEntitlementId());
    }

    @Test
    public void withShopperID() {
        String shopperId = "testShopper";
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withShopperID(shopperId).build();
        assertEquals(shopperId, credit.getShopperId());
    }

    @Test
    public void withoutShopperID() {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getShopperId());
    }

    @Test
    public void withResellerID() {
        String resellerId = "123";
        VirtualMachineCredit credit = new Builder(dataCenterService)
                .withResellerID(resellerId).build();
        assertEquals(resellerId, credit.getResellerId());
    }

    @Test
    public void withoutResellerID() {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getResellerId());
    }

    @Test
    public void havingAccountStatus() {
        AccountStatus status = AccountStatus.ACTIVE;
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withAccountStatus(status).build();
        assertEquals(AccountStatus.ACTIVE, credit.getAccountStatus());
    }

    @Test
    public void withoutAccountStatus() {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getAccountStatus());
    }

    @Test
    public void planFeatures() {
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withPlanFeatures(planFeatures).build();
        assertEquals(tier, credit.getTier());
        assertEquals(managedLevel, credit.getManagedLevel());
        assertEquals(monitoring, credit.getMonitoring());
        assertEquals(operatingSystem, credit.getOperatingSystem());
        assertEquals(controlPanel, credit.getControlPanel());
    }

    @Test
    public void withProductMeta() {
        VirtualMachineCredit credit = new Builder(dataCenterService)
                .withProductMeta(productMeta).build();
        assertEquals(dc, credit.getDataCenter());
        assertEquals(provisionDate, credit.getProvisionDate());
        assertEquals(fullyManagedEmailSent, credit.isFullyManagedEmailSent());
        assertEquals(planChangePending, credit.isPlanChangePending());
        assertEquals(productId, credit.getProductId());
    }


}