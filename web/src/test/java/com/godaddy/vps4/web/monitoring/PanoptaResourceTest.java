package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServers;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class PanoptaResourceTest {

    private Config config = mock(Config.class);
    private VmResource vmResource = mock(VmResource.class);
    private PanoptaService panoptaService = mock(PanoptaService.class);
    private PanoptaApiCustomerService panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
    private PanoptaApiServerService panoptaApiServerService = mock(PanoptaApiServerService.class);
    private CreditService creditService = mock(CreditService.class);
    private PanoptaCustomer panoptaCustomer = mock(PanoptaCustomer.class);
    private DataCenterService dataCenterService = mock(DataCenterService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private ServerSpec serverSpec = mock(ServerSpec.class);
    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private Response.StatusType responseStatusType = mock(Response.StatusType.class);
    private PanoptaResource.CreateCustomerRequest createCustomerRequest;

    private GDUser user = GDUserMock.createShopper();

    private VirtualMachineCredit credit;
    private VirtualMachine virtualMachine;
    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
   private String serverKey = "fake-server-key";

    @Inject
    private PanoptaServers fakePanoptaApiServers;

    private Injector injector = Guice.createInjector(
            new ObjectMapperModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Config.class).toInstance(config);
                    bind(PanoptaService.class).toInstance(panoptaService);
                    bind(PanoptaApiCustomerService.class).toInstance(panoptaApiCustomerService);
                    bind(PanoptaApiServerService.class).toInstance(panoptaApiServerService);
                    bind(PanoptaDataService.class).toInstance(panoptaDataService);
                    bind(CreditService.class).toInstance(creditService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                    bind(VmResource.class).toInstance(vmResource);
                    bind(GDUser.class).toInstance(user);
                }
            });

    private PanoptaResource panoptaResource = injector.getInstance(PanoptaResource.class);

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        credit = createCredit(user.getShopperId(), AccountStatus.ACTIVE, "10");
        virtualMachine =
                new VirtualMachine(vmId, 123L, orionGuid, 321L, new ServerSpec(), "TestVm", null, null,
                                   Instant.now(), null, Instant.MAX, null, 0, UUID.randomUUID());
        when(vmResource.getVm(vmId)).thenReturn(virtualMachine);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
        createCustomerRequest = new PanoptaResource.CreateCustomerRequest();
        try {
            when(panoptaService.createCustomer(any(UUID.class))).thenReturn(panoptaCustomer);
        } catch (PanoptaServiceException psex) {
            fail("Could not setup test. " + psex.getMessage());
        }
    }

    private VirtualMachineCredit createCredit(String shopperId, AccountStatus accountStatus, String tier) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put(ECommCreditService.PlanFeatures.TIER.toString(), tier);

        return new VirtualMachineCredit.Builder(dataCenterService)
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(shopperId)
                .withPlanFeatures(planFeatures)
                .build();
    }

    @Test
    public void testCustomerCreation() {
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(virtualMachineService.getSpec(anyInt())).thenReturn(serverSpec);
        when(serverSpec.isVirtualMachine()).thenReturn(true);
        when(config.get(eq("panopta.api.partner.customer.key.prefix"))).thenReturn("gdtest_");
        when(config.get(eq("panopta.api.customer.email"), anyString())).thenReturn("dev-vps4@godaddy.com");
        when(config.get(eq("panopta.api.package.FULLY_MANAGED"))).thenReturn("godaddy.fully_managed");
        when(responseStatusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        createCustomerRequest.vmId = vmId.toString();

        panoptaResource.createCustomer(createCustomerRequest);

        try {
            verify(panoptaService, times(1)).createCustomer(any(UUID.class));
        } catch (PanoptaServiceException psex) {
            fail("Unexpected exception encountered. " + psex);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throwsExceptionIfCreditIsForDedicatedServer() throws PanoptaServiceException {
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(virtualMachineService.getSpec(anyInt())).thenReturn(serverSpec);
        when(serverSpec.isVirtualMachine()).thenReturn(false);
        createCustomerRequest.vmId = vmId.toString();

        panoptaResource.createCustomer(createCustomerRequest);

        verify(panoptaService, never()).createCustomer(any(UUID.class));
    }

    @Test
    public void testCustomerDeletion() {
        String customerKey = "2hum-wpmt-vswt-2g3b";
        PanoptaDetail panoptaDetails = new PanoptaDetail(vmId, "partnerCustomerKey", customerKey,
                23, "serverKey", Instant.now(), null);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetails);
        panoptaResource.deleteCustomer(vmId);

        verify(panoptaService, times(1)).deleteCustomer(vmId);
    }

    @Test
    public void testGetServer() {
        when(config.get(eq("panopta.api.partner.customer.key.prefix"))).thenReturn("gdtest_");
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(responseStatusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);

        panoptaResource.getServer(serverKey);

        try {
            verify(panoptaService, times(1)).getServer(eq(user.getShopperId()), eq(serverKey));
        } catch (PanoptaServiceException psex) {
            fail("Unexpected exception encountered. " + psex);
        }
    }
}
