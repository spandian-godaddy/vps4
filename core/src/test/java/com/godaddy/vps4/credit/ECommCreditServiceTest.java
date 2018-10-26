package com.godaddy.vps4.credit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.godaddy.vps4.vm.AccountStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.ECommCreditService.ProductMetaField;
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
        account.product_meta.put(ProductMetaField.PRODUCT_ID.toString(), UUID.randomUUID().toString());
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
    public void testGetCreditMapsAccount() throws Exception {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);

        assertEquals(orionGuid, credit.orionGuid);
        assertEquals(Integer.parseInt(account.plan_features.get("tier")), credit.tier);
        assertEquals(Integer.parseInt(account.plan_features.get("managed_level")), credit.managedLevel);
        assertEquals(account.plan_features.get("operatingsystem"), credit.operatingSystem);
        assertEquals(account.plan_features.get("control_panel_type"), credit.controlPanel);
        assertEquals(null, credit.provisionDate);
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
        assertNull(newProdMeta.from.get("data_center"));
        assertNull(newProdMeta.from.get("provision_date"));
        assertEquals(String.valueOf(phx3), newProdMeta.to.get("data_center"));
        assertNotNull(newProdMeta.to.get("provision_date"));
    }

    @Test
    public void testUnclaimCreditCallsUpdateProdMeta() throws Exception {
        String phx3 = "1";
        String provisionDate = Instant.now().toString();
        account.product_meta.put("data_center", phx3);
        account.product_meta.put("provision_date", provisionDate);

        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.unclaimVirtualMachineCredit(orionGuid);

        ArgumentCaptor<MetadataUpdate> argument = ArgumentCaptor.forClass(MetadataUpdate.class);
        verify(ecommService).updateProductMetadata(eq(orionGuid.toString()), argument.capture());
        MetadataUpdate newProdMeta = argument.getValue();
        assertEquals(phx3, newProdMeta.from.get("data_center"));
        assertEquals(provisionDate, newProdMeta.from.get("provision_date"));
        assertNull(newProdMeta.to.get("data_center"));
        assertNull(newProdMeta.to.get("provision_date"));
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
    public void setStatusTest() {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.setStatus(orionGuid, AccountStatus.ABUSE_SUSPENDED);
        account.status = Account.Status.abuse_suspended;
        verify(ecommService).updateAccount(orionGuid.toString(), account);
    }
}
