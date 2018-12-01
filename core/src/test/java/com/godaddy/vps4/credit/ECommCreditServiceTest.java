package com.godaddy.vps4.credit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.ECommCreditService.PlanFeatures;
import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommDataCache;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;

public class ECommCreditServiceTest {

    ECommService ecommService = mock(ECommService.class);
    DataCenterService dcService = mock(DataCenterService.class);
    CreditService creditService = new ECommCreditService(ecommService, dcService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(ECommService.class).toInstance(ecommService);
    });

    private Account account;
    private UUID orionGuid;

    // Initial product meta data
    private UUID vmId = UUID.randomUUID();
    private String provisionDate = Instant.now().toString();
    private String dcId = "3";

    @Before
    public void setUp() throws Exception {
        orionGuid = UUID.randomUUID();
        account = new Account();
        account.account_guid = orionGuid.toString();
        account.product = "vps4";
        account.shopper_id = "uniq-shopper-id";
        account.status = Account.Status.active;
        account.plan_features = new HashMap<>();
        account.plan_features.put("tier", "10");
        account.plan_features.put("managed_level", "1");
        account.plan_features.put("monitoring", "0");
        account.plan_features.put("operatingsystem", "linux");
        account.plan_features.put("control_panel_type", "cpanel");
        account.product_meta = new HashMap<>();
    }

    private void markCreditClaimed() {
        account.product_meta.put(ProductMetaField.PRODUCT_ID.toString(), vmId.toString());
        account.product_meta.put(ProductMetaField.DATA_CENTER.toString(), dcId);
        account.product_meta.put(ProductMetaField.PROVISION_DATE.toString(), provisionDate);
    }

    @Test
    public void testGetCreditCallsGetAccount() throws Exception {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.getVirtualMachineCredit(orionGuid);
        verify(ecommService).getAccount(eq(orionGuid.toString()));
    }

    @Test
    public void testGetCreditIncludesDataCenter(){
        DataCenter dc = new DataCenter(1, "phx3");
        account.product_meta.put("data_center", String.valueOf(dc.dataCenterId));
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertEquals(orionGuid, credit.orionGuid);
    }

    @Test
    public void testGetCreditIncludesProductId(){
        UUID vmId = UUID.randomUUID();
        account.product_meta.put("product_id", vmId.toString());
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertEquals(vmId, credit.productId);
    }

    @Test
    public void testGetCreditNoAccountFoundReturnsNull() {
        when(ecommService.getAccount(orionGuid.toString())).thenThrow(new WebApplicationException());
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        assertNull(credit);
    }

    @Test
    public void testGetCreditExceptionSwallowedDuringCreditToVmMapping() {
        account.plan_features.put(PlanFeatures.TIER.toString(), "nonInt");
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.getVirtualMachineCredit(orionGuid);
    }

    @Test(expected=RuntimeException.class)
    public void testGetCreditSqlExceptionThrown() {
        markCreditClaimed();
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        when(dcService.getDataCenter(anyInt())).thenThrow(new RuntimeException("Sql.exec exception"));
        creditService.getVirtualMachineCredit(orionGuid);
    }

    @Test
    public void testGetCreditMapsAccount() throws Exception {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertEquals(orionGuid, credit.orionGuid);
        assertEquals(Integer.parseInt(account.plan_features.get("tier")), credit.tier);
        assertEquals(Integer.parseInt(account.plan_features.get("managed_level")), credit.managedLevel);
        assertEquals(account.plan_features.get("operatingsystem"), credit.operatingSystem);
        assertEquals(account.plan_features.get("control_panel_type"), credit.controlPanel);
        assertNull(credit.provisionDate);
        assertEquals(account.shopper_id, credit.shopperId);
    }

    @Test
    public void testGetBrandResellerCredit() throws Exception {
        // Brand Resellers set shopper_id to match sub_account_shopper_id
        account.sub_account_shopper_id = "brand-reseller-shopper-id";
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertEquals(account.sub_account_shopper_id, credit.shopperId);
    }

    @Test
    public void testGetCreditWithProvisionDate() throws Exception {
        Instant testDate = Instant.now();
        account.product_meta.put("provision_date", testDate.toString());
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertEquals(testDate, credit.provisionDate);
    }

    @Test
    public void testGetCreditsForAccountNotFound() throws Exception {
        when(ecommService.getAccount(orionGuid.toString()))
            .thenThrow(new RuntimeException("Fake account not found"));
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertNull(credit);
    }

    @Test
    public void testGetUnclaimedCreditsCallsGetAccounts() throws Exception {
        assertTrue(creditService.getUnclaimedVirtualMachineCredits(account.shopper_id).isEmpty());
        verify(ecommService).getAccounts(eq(account.shopper_id));
    }

    @Test
    public void testGetUnclaimedCreditsFiltersActive() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getUnclaimedVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());

        account.status = Account.Status.removed;
        credits = creditService.getUnclaimedVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());
    }

    @Test
    public void testGetUnclaimedCreditsFiltersUnclaimed() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getUnclaimedVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());

        account.product_meta.put("data_center", "1");
        credits = creditService.getUnclaimedVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());
    }

    @Test
    public void testGetCreditsCallsGetAccounts() throws Exception {
        assertTrue(creditService.getVirtualMachineCredits(account.shopper_id).isEmpty());
        verify(ecommService).getAccounts(eq(account.shopper_id));
    }

    @Test
    public void testGetCreditsFiltersRemoved() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());

        account.status = Account.Status.removed;
        credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());
    }

    @Test
    public void testGetCreditsDoesntFilterUnclaimed() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());

        account.product_meta.put("data_center", "1");
        credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());
    }

    @Test
    public void testGetCreditsFiltersOtherProducts() {
        account.product = "mwp2";
        account.plan_features = new HashMap<>();
        account.plan_features.put("plan_type", "pro5");
        account.plan_features.put("max_sites", "5");
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());

        credits = creditService.getUnclaimedVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());
    }


    @Test
    public void testCreateCreditCallsCreateAccount() throws Exception {
        creditService.createVirtualMachineCredit(orionGuid, account.shopper_id, "linux", "cpanel", 10, 1, 0, 1);

        ArgumentCaptor<Account> argument = ArgumentCaptor.forClass(Account.class);
        verify(ecommService).createAccount(argument.capture());
        Account newAccount = argument.getValue();
        assertEquals(orionGuid.toString(), newAccount.account_guid);
        assertEquals(account.shopper_id, newAccount.shopper_id);
        assertEquals("10", newAccount.plan_features.get("tier"));
        assertEquals("1", newAccount.plan_features.get("managed_level"));
        assertEquals("linux", newAccount.plan_features.get("operatingsystem"));
        assertEquals("cpanel", newAccount.plan_features.get("control_panel_type"));
    }

    @Test
    public void testClaimCreditCallsUpdateProdMeta() throws Exception {
        int phx3 = 1;
        UUID vmId = UUID.randomUUID();
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.claimVirtualMachineCredit(orionGuid, phx3, vmId);

        ArgumentCaptor<MetadataUpdate> argument = ArgumentCaptor.forClass(MetadataUpdate.class);
        verify(ecommService).updateProductMetadata(eq(orionGuid.toString()), argument.capture());
        MetadataUpdate newProdMeta = argument.getValue();
        // verify same number fields in to and from
        assertEquals(newProdMeta.from.size(), newProdMeta.to.size());
        // from fields are null
        assertNull(newProdMeta.from.get("product_id"));
        assertNull(newProdMeta.from.get("data_center"));
        assertNull(newProdMeta.from.get("provision_date"));
        // to fields are set
        assertEquals(vmId.toString(), newProdMeta.to.get("product_id"));
        assertEquals(String.valueOf(phx3), newProdMeta.to.get("data_center"));
        assertNotNull(newProdMeta.to.get("provision_date"));
    }

    @Test
    public void testUnclaimCreditCallsUpdateProdMeta() {
        markCreditClaimed();
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.unclaimVirtualMachineCredit(orionGuid, vmId);

        ArgumentCaptor<MetadataUpdate> argument = ArgumentCaptor.forClass(MetadataUpdate.class);
        verify(ecommService).updateProductMetadata(eq(orionGuid.toString()), argument.capture());
        MetadataUpdate newProdMeta = argument.getValue();
        assertEquals(dcId, newProdMeta.from.get(ProductMetaField.DATA_CENTER.toString()));
        assertEquals(provisionDate, newProdMeta.from.get(ProductMetaField.PROVISION_DATE.toString()));
        assertEquals(vmId.toString(), newProdMeta.from.get(ProductMetaField.PRODUCT_ID.toString()));
        assertNull(newProdMeta.to.get(ProductMetaField.DATA_CENTER.toString()));
        assertNull(newProdMeta.to.get(ProductMetaField.PROVISION_DATE.toString()));
        assertNull(newProdMeta.to.get(ProductMetaField.PRODUCT_ID.toString()));
    }

    @Test
    public void testUnclaimCreditWithNonMatchingVmId() {
        markCreditClaimed();
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        when(ecommService.updateProductMetadata(eq(orionGuid.toString()), any(MetadataUpdate.class)))
            .thenThrow(new WebApplicationException());
        UUID someVmId = UUID.randomUUID();
        creditService.unclaimVirtualMachineCredit(orionGuid, someVmId);

        ArgumentCaptor<MetadataUpdate> argument = ArgumentCaptor.forClass(MetadataUpdate.class);
        verify(ecommService).updateProductMetadata(eq(orionGuid.toString()), argument.capture());
        MetadataUpdate newProdMeta = argument.getValue();
        assertEquals(someVmId.toString(), newProdMeta.from.get(ProductMetaField.PRODUCT_ID.toString()));
    }

    @Test
    public void testSetCommonNameCallsSetCommonName() throws Exception {
        creditService.setCommonName(orionGuid, "New Common Name");
        ECommDataCache dc = new ECommDataCache();
        dc.common_name = "New Common Name";
        verify(ecommService).setCommonName(orionGuid.toString(), dc);
    }

    @Test
    public void testGetProductMeta() {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        Map<ProductMetaField, String> productMeta = creditService.getProductMeta(orionGuid);
        assertEquals(account.product_meta.get(ProductMetaField.PRODUCT_ID.toString()), productMeta.get(ProductMetaField.PRODUCT_ID));
    }

    @Test
    public void testUpdateProductMetaSingleField() {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);

        ProductMetaField field = ProductMetaField.PLAN_CHANGE_PENDING;
        creditService.updateProductMeta(orionGuid, field, "true");

        ArgumentCaptor<MetadataUpdate> argument = ArgumentCaptor.forClass(MetadataUpdate.class);
        verify(ecommService).updateProductMetadata(eq(orionGuid.toString()), argument.capture());
        assertNull(argument.getValue().from.get(field.toString()));
        assertEquals("true", argument.getValue().to.get(field.toString()));
    }

    @Test
    public void setStatusTest() {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);

        Map<AccountStatus, Account.Status> mapStatusToHfs = new EnumMap<>(AccountStatus.class);
        mapStatusToHfs.put(AccountStatus.ABUSE_SUSPENDED, Account.Status.abuse_suspended);
        mapStatusToHfs.put(AccountStatus.ACTIVE, Account.Status.active);
        mapStatusToHfs.put(AccountStatus.REMOVED, Account.Status.removed);
        mapStatusToHfs.put(AccountStatus.SUSPENDED, Account.Status.suspended);

        ArgumentCaptor<Account> argument = ArgumentCaptor.forClass(Account.class);
        mapStatusToHfs.forEach((k,v) -> {
            creditService.setStatus(orionGuid, k);
            verify(ecommService, atLeastOnce()).updateAccount(eq(orionGuid.toString()), argument.capture());
            assertEquals(v, argument.getValue().status);
        });

    }

}
