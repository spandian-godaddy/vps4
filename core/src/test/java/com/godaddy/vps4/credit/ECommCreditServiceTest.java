package com.godaddy.vps4.credit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.security.Vps4User;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.vhfs.ecomm.Account;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.ecomm.MetadataUpdate;

public class ECommCreditServiceTest {

    ECommService ecommService = mock(ECommService.class);
    CreditService creditService = new ECommCreditService(ecommService);

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
    }

    @Test
    public void testGetCreditCallsGetAccount() throws Exception {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        creditService.getVirtualMachineCredit(orionGuid);
        verify(ecommService).getAccount(eq(orionGuid.toString()));
    }

    @Test
    public void testGetCreditMapsAccount() throws Exception {
        when(ecommService.getAccount(orionGuid.toString())).thenReturn(account);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(orionGuid);
        System.out.println(credit.toString());

        assertEquals(orionGuid, credit.orionGuid);
        assertEquals(Integer.parseInt(account.plan_features.get("tier")), credit.tier);
        assertEquals(Integer.parseInt(account.plan_features.get("managed_level")), credit.managedLevel);
        assertEquals(account.plan_features.get("os"), credit.operatingSystem);
        assertEquals(account.plan_features.get("control_panel_type"), credit.controlPanel);
        assertEquals(null, credit.createDate);
        assertEquals(null, credit.provisionDate);
        assertEquals(account.shopper_id, credit.shopperId);
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
    public void testGetCreditsCallsGetAccounts() throws Exception {
        assertTrue(creditService.getVirtualMachineCredits(account.shopper_id).isEmpty());
        verify(ecommService).getAccounts(eq(account.shopper_id));
    }

    @Test
    public void testGetCreditsFiltersActive() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());

        account.status = Account.Status.removed;
        credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());
    }

    @Test
    public void testGetCreditsFiltersUnclaimed() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));

        List<VirtualMachineCredit> credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(1, credits.size());

        account.product_meta.put("data_center", "phx");
        credits = creditService.getVirtualMachineCredits(account.shopper_id);
        assertEquals(0, credits.size());
    }

    @Test
    public void testCreateCreditCallsCreateAccount() throws Exception {
        creditService.createVirtualMachineCredit(orionGuid, "linux", "cpanel", 10, 1, account.shopper_id);

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
    public void testCreateCreditIfNoneExists() throws Exception {
        Vps4User vps4User = new Vps4User(1, account.shopper_id);
        creditService.createCreditIfNoneExists(vps4User);
        verify(ecommService).getAccounts(account.shopper_id);
        verify(ecommService).createAccount(any(Account.class));
    }

    @Test
    public void testCreditAlreadyExists() throws Exception {
        when(ecommService.getAccounts(account.shopper_id)).thenReturn(Arrays.asList(account));
        Vps4User vps4User = new Vps4User(1, account.shopper_id);
        creditService.createCreditIfNoneExists(vps4User);
        verify(ecommService).getAccounts(account.shopper_id);
        verify(ecommService, never()).createAccount(any(Account.class));
    }

    @Test
    public void testClaimCreditCallsUpdateProdMeta() throws Exception {
        int phx3 = 1;
        creditService.claimVirtualMachineCredit(orionGuid, phx3);

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
}