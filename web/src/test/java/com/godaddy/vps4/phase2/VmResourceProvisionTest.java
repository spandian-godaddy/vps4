package com.godaddy.vps4.phase2;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmAction;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmResource.ProvisionVmRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmResourceProvisionTest {

    @Inject DataSource dataSource;

    private GDUser user;
    private CreditService creditService;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest() {
        System.setProperty("hfs.sgid.prefix", SqlTestData.TEST_VM_SGID);
        injector.injectMembers(this);
        creditService = injector.getInstance(CreditService.class);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmResource getVmResource() {
        return injector.getInstance(VmResource.class);
    }

    private VirtualMachineCredit createVmCredit(String controlPanel,
            boolean claimed, AccountStatus accountStatus) {
        UUID newGuid = UUID.randomUUID();
        Instant provisionDate = claimed ? Instant.now() : null;
        return new VirtualMachineCredit(newGuid, 10, 1, 0, "linux", controlPanel, provisionDate, GDUserMock.DEFAULT_SHOPPER, accountStatus,
                null, null, false);
    }

    private ProvisionVmRequest createProvisionVmRequest(UUID orionGuid) {
        ProvisionVmRequest request = new ProvisionVmRequest();
        request.orionGuid = orionGuid;
        request.dataCenterId = 1;
        request.image = "hfs-centos-7";
        request.name = SqlTestData.TEST_VM_NAME;
        request.password = "Password1!";
        return request;
    }

    public void testProvisionVm() throws InterruptedException {
        VirtualMachineCredit credit = createVmCredit("myh", false, AccountStatus.ACTIVE);
        ProvisionVmRequest request = createProvisionVmRequest(credit.orionGuid);
        Mockito.when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        VmAction vmAction = getVmResource().provisionVm(request);
        Assert.assertNotNull(vmAction.commandId);
    }

    // === provisionVm Tests ===
    @Test
    public void testShopperProvisionVm() throws InterruptedException {
        testProvisionVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperProvisionVm() throws InterruptedException {
        user = GDUserMock.createShopper("shopperX");
        testProvisionVm();
    }

    @Test(expected=Vps4NoShopperException.class)
    public void testAdminFailsProvisionVm() throws InterruptedException {
        user = GDUserMock.createAdmin();
        testProvisionVm();
    }

    @Test
    public void testE2SProvisionVm() throws InterruptedException {
        user = GDUserMock.createEmployee2Shopper();
        testProvisionVm();
    }

    @Test
    public void testProvisionVmInvalidCredit() throws InterruptedException {
        // Credit doesn't match provision request image
        VirtualMachineCredit credit = createVmCredit("cpanel", false, AccountStatus.ACTIVE);
        ProvisionVmRequest request = createProvisionVmRequest(credit.orionGuid);
        Mockito.when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        try {
            getVmResource().provisionVm(request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testProvisionVmNoSuchCredit() throws InterruptedException {
        UUID creditGuid = UUID.randomUUID();
        ProvisionVmRequest request = createProvisionVmRequest(creditGuid);
        Mockito.when(creditService.getVirtualMachineCredit(creditGuid)).thenReturn(null);

        try {
            getVmResource().provisionVm(request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_NOT_FOUND", e.getId());
        }
    }

    @Test
    public void testProvisionVmCreditClaimed() throws InterruptedException {
        VirtualMachineCredit credit = createVmCredit("cpanel", true, AccountStatus.ACTIVE);
        ProvisionVmRequest request = createProvisionVmRequest(credit.orionGuid);
        Mockito.when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        try {
            getVmResource().provisionVm(request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_ALREADY_IN_USE", e.getId());
        }
    }

    @Test
    public void testSuspendedShopperProvisionVm() throws InterruptedException {
        VirtualMachineCredit credit = createVmCredit("myh", false, AccountStatus.SUSPENDED);
        ProvisionVmRequest request = createProvisionVmRequest(credit.orionGuid);
        Mockito.when(creditService.getVirtualMachineCredit(credit.orionGuid)).thenReturn(credit);

        try {
            getVmResource().provisionVm(request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }
}
