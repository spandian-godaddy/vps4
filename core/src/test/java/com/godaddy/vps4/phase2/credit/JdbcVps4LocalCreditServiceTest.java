package com.godaddy.vps4.phase2.credit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.Vps4LocalCreditService;
import com.godaddy.vps4.credit.jdbc.JdbcVps4LocalCreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class JdbcVps4LocalCreditServiceTest {
    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    Vps4LocalCreditService vps4LocalCreditService = new JdbcVps4LocalCreditService(dataSource);

    List<VirtualMachineCredit> credits;

    @Before
    public void setUp() {
        credits = new ArrayList<>();
    }

    @After
    public void tearDown() {
        for (VirtualMachineCredit virtualMachineCredit : credits) {
            vps4LocalCreditService.deleteCredit(virtualMachineCredit.entitlementData.entitlementId);
        }
    }

    @Test
    public void testInsertCredit() {
        VirtualMachineCredit testCredit = getTestCredit();

        vps4LocalCreditService.insertCredit(testCredit);

        VirtualMachineCredit credit = vps4LocalCreditService.getCredit(testCredit.entitlementData.entitlementId);
        assertNotNull(credit);
        compareCredits(testCredit, credit);
    }

    @Test
    public void testInsertEmptyCredit() {
        VirtualMachineCredit testCredit = getEmptyCredit();

        vps4LocalCreditService.insertCredit(testCredit);

        VirtualMachineCredit credit = vps4LocalCreditService.getCredit(testCredit.entitlementData.entitlementId);
        assertNotNull(credit);
        compareCredits(testCredit, credit);
    }

    @Test
    public void testGetcreditByVmId() {
        VirtualMachineCredit testCredit = getTestCredit();
        vps4LocalCreditService.insertCredit(testCredit);

        VirtualMachineCredit credit = vps4LocalCreditService.getCreditByVmId(testCredit.prodMeta.productId);

        assertNotNull(credit);
        compareCredits(testCredit, credit);
    }

    @Test
    public void testGetcreditByShopperId() {
        VirtualMachineCredit testCredit1 = getTestCredit();
        VirtualMachineCredit testCredit2 = getTestCredit();
        testCredit1.shopperId = testCredit2.shopperId;
        vps4LocalCreditService.insertCredit(testCredit1);
        vps4LocalCreditService.insertCredit(testCredit2);

        List<VirtualMachineCredit> credits = vps4LocalCreditService.getCreditByShopperId(testCredit2.shopperId);
        
        assertNotNull(credits);
        compareCredits(testCredit1, credits.get(0));
        compareCredits(testCredit2, credits.get(1));
    }

    @Test
    public void testGetcreditByCustomerId() {
        VirtualMachineCredit testCredit1 = getTestCredit();
        VirtualMachineCredit testCredit2 = getTestCredit();
        testCredit1.entitlementData.customerId = testCredit2.entitlementData.customerId;
        vps4LocalCreditService.insertCredit(testCredit1);
        vps4LocalCreditService.insertCredit(testCredit2);

        List<VirtualMachineCredit> credits = vps4LocalCreditService.getCreditByCustomerId(testCredit2.entitlementData.customerId);
        
        assertNotNull(credits);
        compareCredits(testCredit1, credits.get(0));
        compareCredits(testCredit2, credits.get(1));
    }

    @Test
    public void testUpdateCredit() {
        VirtualMachineCredit testCredit = getTestCredit();
        vps4LocalCreditService.insertCredit(testCredit);
        VirtualMachineCredit updatedCredit = updateTestCredit(testCredit);

        vps4LocalCreditService.updateCredit(updatedCredit);
        VirtualMachineCredit credit = vps4LocalCreditService.getCredit(testCredit.entitlementData.entitlementId);

        compareCredits(updatedCredit, credit);

        assertNotNull(credit);
        credit = vps4LocalCreditService.getCredit(testCredit.entitlementData.entitlementId);
    }

    @Test
    public void testDeleteCredit() {
        VirtualMachineCredit testCredit = getTestCredit();
        vps4LocalCreditService.insertCredit(testCredit);

        vps4LocalCreditService.deleteCredit(testCredit.entitlementData.entitlementId);

        VirtualMachineCredit credit = vps4LocalCreditService.getCredit(testCredit.entitlementData.entitlementId);
        Assert.assertNull(credit);
    }

    private void compareCredits(VirtualMachineCredit testCredit, VirtualMachineCredit credit) {
        assertEquals(testCredit.entitlementData.entitlementId, credit.entitlementData.entitlementId);
        assertEquals(testCredit.entitlementData.tier, credit.entitlementData.tier);
        assertEquals(testCredit.entitlementData.managedLevel, credit.entitlementData.managedLevel);
        assertEquals(testCredit.entitlementData.operatingSystem, credit.entitlementData.operatingSystem);
        assertEquals(testCredit.entitlementData.controlPanel, credit.entitlementData.controlPanel);
        if(credit.prodMeta.provisionDate != null)
            assert credit.prodMeta.provisionDate.isBefore(Instant.now());
        assertEquals(testCredit.shopperId, credit.shopperId);
        assertEquals(testCredit.entitlementData.monitoring, credit.entitlementData.monitoring);
        assertEquals(testCredit.entitlementData.accountStatus, credit.entitlementData.accountStatus);
        assertEquals(testCredit.prodMeta.dataCenter, credit.prodMeta.dataCenter);
        assertEquals(testCredit.prodMeta.productId, credit.prodMeta.productId);
        assertEquals(testCredit.prodMeta.fullyManagedEmailSent, credit.prodMeta.fullyManagedEmailSent);
        assertEquals(testCredit.resellerId, credit.resellerId);
        assertEquals(testCredit.entitlementData.pfid, credit.entitlementData.pfid);
        if(credit.prodMeta.purchasedAt != null)
            assert credit.prodMeta.purchasedAt.isBefore(Instant.now());
        assertEquals(testCredit.entitlementData.customerId, credit.entitlementData.customerId);
        if(credit.entitlementData.expireDate != null)
            assert credit.entitlementData.expireDate.isBefore(Instant.now());
        assertEquals(testCredit.entitlementData.mssql, credit.entitlementData.mssql);
        assertEquals(testCredit.entitlementData.cdnWaf, credit.entitlementData.cdnWaf);
    }

    private VirtualMachineCredit getTestCredit() {
        VirtualMachineCredit testCredit = new VirtualMachineCredit();
        testCredit.entitlementData.entitlementId = UUID.randomUUID();
        testCredit.entitlementData.tier = 1;
        testCredit.entitlementData.managedLevel = 1;
        testCredit.entitlementData.operatingSystem = "os";
        testCredit.entitlementData.controlPanel = "cp";
        testCredit.prodMeta.provisionDate = Instant.now();
        testCredit.shopperId = "testCreditShopperId";
        testCredit.entitlementData.monitoring = 1;
        testCredit.entitlementData.accountStatus = AccountStatus.ACTIVE;
        testCredit.prodMeta.dataCenter = 1;
        testCredit.prodMeta.productId = UUID.randomUUID();
        testCredit.prodMeta.fullyManagedEmailSent = true;
        testCredit.resellerId = "1";
        testCredit.entitlementData.pfid = 123;
        testCredit.prodMeta.purchasedAt = Instant.now();
        testCredit.entitlementData.customerId = UUID.randomUUID();
        testCredit.entitlementData.expireDate = Instant.now();
        testCredit.entitlementData.mssql = "mysql";
        testCredit.entitlementData.cdnWaf = 5;
        credits.add(testCredit);
        return testCredit;
    }

    private VirtualMachineCredit updateTestCredit(VirtualMachineCredit testCredit) {
        testCredit.entitlementData.tier = 2;
        testCredit.entitlementData.managedLevel = 2;
        testCredit.entitlementData.operatingSystem = "osUpdated";
        testCredit.entitlementData.controlPanel = "cpUpdated";
        testCredit.prodMeta.provisionDate = Instant.now();
        testCredit.shopperId = "testCreditShopperIdUpdated";
        testCredit.entitlementData.monitoring = 2;
        testCredit.entitlementData.accountStatus = AccountStatus.SUSPENDED;
        testCredit.prodMeta.dataCenter = 2;
        testCredit.prodMeta.productId = UUID.randomUUID();
        testCredit.prodMeta.fullyManagedEmailSent = false;
        testCredit.resellerId = "2";
        testCredit.entitlementData.pfid = 1234;
        testCredit.prodMeta.purchasedAt = Instant.now();
        testCredit.entitlementData.customerId = UUID.randomUUID();
        testCredit.entitlementData.expireDate = Instant.now();
        testCredit.entitlementData.mssql = "mysql2";
        testCredit.entitlementData.cdnWaf = 6;
        return testCredit;
    }

    private VirtualMachineCredit getEmptyCredit() {
        VirtualMachineCredit testCredit = new VirtualMachineCredit();
        testCredit.entitlementData.entitlementId = UUID.randomUUID();
        return testCredit;
    }
}
