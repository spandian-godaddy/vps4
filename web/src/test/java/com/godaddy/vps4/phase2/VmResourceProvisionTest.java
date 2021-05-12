package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmResource.ProvisionVmRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;

public class VmResourceProvisionTest {

    @Inject DataSource dataSource;

    private GDUser user;
    private CreditService creditService;
    private CommandService mockCmdService;

    private Map<String, String> planFeatures;
    private Map<String, String> productMeta;
    private UUID orionGuid;
    private String resellerId;
    private AccountStatus accountStatus;
    private ProvisionVmRequest request;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    SchedulerWebService swServ = mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
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
        mockCmdService = injector.getInstance(CommandService.class);

        orionGuid = UUID.randomUUID();
        resellerId = "1";
        accountStatus = AccountStatus.ACTIVE;

        user = GDUserMock.createShopper();
        planFeatures = new HashMap<>();
        planFeatures.put("tier", String.valueOf(10));
        planFeatures.put("managed_level", String.valueOf(1));
        planFeatures.put("control_panel_type", "myh");
        planFeatures.put("monitoring", String.valueOf(0));
        planFeatures.put("operatingsystem", "linux");

        productMeta = new HashMap<>();

        request = new ProvisionVmRequest();
        request.orionGuid = orionGuid;
        request.dataCenterId = 1;
        request.image = "hfs-centos-7";
        request.name = SqlTestData.TEST_VM_NAME;
        request.password = "Password1!";
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmResource getVmResource() {
        return injector.getInstance(VmResource.class);
    }

    private VirtualMachineCredit createVmCredit() {
        return new VirtualMachineCredit.Builder(mock(DataCenterService.class))
            .withAccountGuid(orionGuid.toString())
            .withAccountStatus(accountStatus)
            .withShopperID(GDUserMock.DEFAULT_SHOPPER)
            .withResellerID(resellerId)
            .withProductMeta(productMeta)
            .withPlanFeatures(planFeatures)
            .build();
    }

    private void testProvisionVm() {
        VirtualMachineCredit credit = createVmCredit();
        when(creditService.getVirtualMachineCredit(credit.getOrionGuid())).thenReturn(credit);

        VmAction vmAction = getVmResource().provisionVm(request);
        Assert.assertNotNull(vmAction.commandId);
    }

    // === provisionVm Tests ===
    @Test
    public void testShopperProvisionVm() {
        testProvisionVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperProvisionVm() {
        user = GDUserMock.createShopper("shopperX");
        testProvisionVm();
    }

    @Test(expected=Vps4NoShopperException.class)
    public void testAdminFailsProvisionVm() {
        user = GDUserMock.createAdmin();
        testProvisionVm();
    }

    @Test
    public void testE2SProvisionVm() {
        user = GDUserMock.createEmployee2Shopper();
        testProvisionVm();
    }

    @Test
    public void testProvisionVmInvalidCreditCP() {
        // Credit doesn't match provision request image
        planFeatures.put("control_panel_type", "cpanel");

        try {
            testProvisionVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testProvisionVmInvalidCreditOS() {
        planFeatures.put("operatingsystem", "windows");

        try {
            testProvisionVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testProvisionVmInvalidCreditDed() {
        // Ded4 credit doesn't match vps image
        planFeatures.put("tier", String.valueOf(60));

        try {
            testProvisionVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testProvisionVmNoSuchCredit() {
        request.orionGuid = UUID.randomUUID();
        when(creditService.getVirtualMachineCredit(request.orionGuid)).thenReturn(null);

        try {
            testProvisionVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_NOT_FOUND", e.getId());
        }
    }

    @Test
    public void testProvisionVmCreditClaimed() {
        planFeatures.put("control_panel_type", "cpanel");
        productMeta.put("provision_date", Instant.now().toString());

        try {
            testProvisionVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CREDIT_ALREADY_IN_USE", e.getId());
        }
    }

    @Test
    public void testSuspendedShopperProvisionVm() {
        accountStatus = AccountStatus.SUSPENDED;

        try {
            testProvisionVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    @Test
    public void testProvisionVmUnsupportedResellerDc() {
        // HEG Reseller is restricted in the reseller_datacenters table to dataCenterID==4
        // VM Create attempts to use dataCenterId=1, so test should fail
        String heartInternet = "525848";
        String tsoHost = "527397";
        String domainFactory = "525847";
        String hostEurope = "525847";

        List<String> emea_brand_resellers = Arrays.asList(heartInternet, tsoHost, domainFactory, hostEurope);
        for (String reseller : emea_brand_resellers) {
            resellerId = reseller;
            try {
                testProvisionVm();
                Assert.fail("DATACENTER_UNSUPPORTED exception expected for reseller id: " + resellerId);
            } catch (Vps4Exception e) {
                Assert.assertEquals("DATACENTER_UNSUPPORTED", e.getId());
            }
        }
    }

    @Test
    public void testProvisionVmSupportedResellerDc() {
        // MT Reseller is restricted in the reseller_datacenters table to dataCenterID==1, so test should succeed
        String MT_RESELLER_ID = "495469";
        resellerId = MT_RESELLER_ID;

        testProvisionVm();
    }

    @Test
    public void testProvisionVMUsesOpenStackCmd() {
        testProvisionVm();

        ArgumentCaptor<CommandGroupSpec> captor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(mockCmdService).executeCommand(captor.capture());
        String provisionCmd = captor.getValue().commands.get(0).command;
        assertEquals("ProvisionVm", provisionCmd);
    }

    @Test
    public void testProvisionVMUsesOptimizedHostingCmd() {
        request.image = "hfs-centos70-x86_64-vmtempl";
        testProvisionVm();

        ArgumentCaptor<CommandGroupSpec> captor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(mockCmdService).executeCommand(captor.capture());
        String provisionCmd = captor.getValue().commands.get(0).command;
        assertEquals("ProvisionOHVm", provisionCmd);
    }

    @Test
    public void testProvisionVMUsesDedicatedCmd() {
        planFeatures.put("tier", String.valueOf(60));
        request.image = "centos7_64";
        testProvisionVm();

        ArgumentCaptor<CommandGroupSpec> captor = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(mockCmdService).executeCommand(captor.capture());
        String provisionCmd = captor.getValue().commands.get(0).command;
        assertEquals("ProvisionDedicated", provisionCmd);
    }
}
