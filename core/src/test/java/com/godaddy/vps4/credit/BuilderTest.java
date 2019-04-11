package com.godaddy.vps4.credit;

import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import gdg.hfs.vhfs.ecomm.Account;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static com.godaddy.vps4.credit.VirtualMachineCredit.Builder;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import static com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import static org.mockito.Mockito.when;


public class BuilderTest {
    final private DataCenterService dataCenterService = mock(DataCenterService.class);

    private Map<String, String> planFeatures;
    final int tier = 10;
    final int managedLevel = 1;
    final int monitoring = 1;
    final String operatingSystem = "linux";
    final String controlPanel = "cpanel";

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
    }

    @Test
    public void withAccountGuid() throws Exception {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withAccountGuid(orionGuid.toString()).build();
        assertEquals(orionGuid, credit.getOrionGuid());
    }

    @Test
    public void withoutAccountGuid() throws Exception {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getOrionGuid());
    }

    @Test
    public void withShopperID() throws Exception {
        String shopperId = "testShopper";
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withShopperID(shopperId).build();
        assertEquals(shopperId, credit.getShopperId());
    }

    @Test
    public void withoutShopperID() throws Exception {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getShopperId());
    }

    @Test
    public void withResellerID() throws Exception {
        String resellerId = "123";
        VirtualMachineCredit credit = new Builder(dataCenterService)
                .withResellerID(resellerId).build();
        assertEquals(resellerId, credit.getResellerId());
    }

    @Test
    public void withoutResellerID() throws Exception {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getResellerId());
    }

    @Test
    public void havingAccountStatus() throws Exception {
        Account.Status status = Account.Status.active;
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withAccountStatus(status).build();
        assertEquals(AccountStatus.ACTIVE, credit.getAccountStatus());
    }

    @Test
    public void withoutAccountStatus() throws Exception {
        VirtualMachineCredit credit = new Builder(dataCenterService).build();
        assertNull(credit.getAccountStatus());
    }

    @Test
    public void planFeatures() throws Exception {
        VirtualMachineCredit credit = new Builder(dataCenterService)
            .withPlanFeatures(planFeatures).build();
        assertEquals(tier, credit.getTier());
        assertEquals(managedLevel, credit.getManagedLevel());
        assertEquals(monitoring, credit.getMonitoring());
        assertEquals(operatingSystem, credit.getOperatingSystem());
        assertEquals(controlPanel, credit.getControlPanel());
    }

    @Test
    public void withProductMeta() throws Exception {
        VirtualMachineCredit credit = new Builder(dataCenterService)
                .withProductMeta(productMeta).build();
        assertEquals(dc, credit.getDataCenter());
        assertEquals(provisionDate, credit.getProvisionDate());
        assertEquals(fullyManagedEmailSent, credit.isFullyManagedEmailSent());
        assertEquals(planChangePending, credit.isPlanChangePending());
        assertEquals(productId, credit.getProductId());
    }


}