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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaApiCustomerList;
import com.godaddy.vps4.panopta.PanoptaApiCustomerRequest;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class PanoptaResourceTest {

    private Config config = mock(Config.class);
    private PanoptaApiCustomerService panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
    private PanoptaApiServerService panoptaApiServerService = mock(PanoptaApiServerService.class);
    private CreditService creditService = mock(CreditService.class);
    private DataCenterService dataCenterService = mock(DataCenterService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private ServerSpec serverSpec = mock(ServerSpec.class);
    private Response response = mock(Response.class);
    private Response.StatusType responseStatusType = mock(Response.StatusType.class);

    private GDUser user = GDUserMock.createShopper();
    private VirtualMachineCredit credit;
    private UUID orionGuid = UUID.randomUUID();
    PanoptaResource.CreateCustomerRequest createCustomerRequest;
    @Inject
    private PanoptaApiCustomerList fakePanoptaApiCustomerList;
    @Inject
    private ObjectMapper objectMapper;

    private Injector injector = Guice.createInjector(
            new ObjectMapperModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Config.class).toInstance(config);
                    bind(PanoptaApiCustomerService.class).toInstance(panoptaApiCustomerService);
                    bind(PanoptaApiServerService.class).toInstance(panoptaApiServerService);
                    bind(CreditService.class).toInstance(creditService);
                    bind(DataCenterService.class).toInstance(dataCenterService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                    bind(ServerSpec.class).toInstance(serverSpec);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            }
            );

    private PanoptaResource panoptaResource = injector.getInstance(PanoptaResource.class);

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        credit = createCredit(AccountStatus.ACTIVE, "10");
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
        createCustomerRequest = new PanoptaResource.CreateCustomerRequest();
        try {
            fakePanoptaApiCustomerList = objectMapper.readValue(mockedupCustomerList(), PanoptaApiCustomerList.class);
        } catch (IOException ex) {
            fail("Could not setup test. " + ex.getMessage());
        }
    }

    private VirtualMachineCredit createCredit(AccountStatus accountStatus, String tier) {
        Map<String, String> planFeatures = new HashMap<>();
        planFeatures.put(ECommCreditService.PlanFeatures.TIER.toString(), tier);

        return new VirtualMachineCredit.Builder(dataCenterService)
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(accountStatus)
                .withShopperID(user.getShopperId())
                .withPlanFeatures(planFeatures)
                .build();
    }

    private String mockedupCustomerList() {
        return "{\n" +
                "  \"customer_list\": [\n" +
                "    {\n" +
                "      \"customer_key\": \"2hum-wpmt-vswt-2g3b\",\n" +
                "      \"email_address\": \"abhoite@godaddy.com\",\n" +
                "      \"name\": \"Godaddy VPS4 POC\",\n" +
                "      \"package\": \"godaddy.fully_managed\",\n" +
                "      \"partner_customer_key\": \"gdtest_" + orionGuid + "\",\n" +
                "      \"status\": \"active\",\n" +
                "      \"url\": \"https://api2.panopta.com/v2/customer/2hum-wpmt-vswt-2g3b\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"meta\": {\n" +
                "    \"limit\": 50,\n" +
                "    \"next\": null,\n" +
                "    \"offset\": 0,\n" +
                "    \"previous\": null,\n" +
                "    \"total_count\": 1\n" +
                "  }\n" +
                "}\n";
    }

    @Test
    public void testPanoptaCustomerCreation() {
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(virtualMachineService.getSpec(anyInt())).thenReturn(serverSpec);
        when(serverSpec.isVirtualMachine()).thenReturn(true);
        when(config.get(eq("panopta.api.partner.customer.key.prefix"))).thenReturn("gdtest_");
        when(config.get(eq("panopta.api.customer.email"), anyString())).thenReturn("dev-vps4@godaddy.com");
        when(config.get(eq("panopta.api.package.FULLY_MANAGED"))).thenReturn("godaddy.fully_managed");
        when(responseStatusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        when(panoptaApiCustomerService.getCustomer(anyString())).thenReturn(fakePanoptaApiCustomerList);
        createCustomerRequest.orionGuid = orionGuid.toString();

        panoptaResource.createCustomer(createCustomerRequest);

        verify(panoptaApiCustomerService, times(1)).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throwsExceptionIfCreditIsForDedicatedServer() {
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(virtualMachineService.getSpec(anyInt())).thenReturn(serverSpec);
        when(serverSpec.isVirtualMachine()).thenReturn(false);
        createCustomerRequest.orionGuid = orionGuid.toString();

        panoptaResource.createCustomer(createCustomerRequest);

        verify(panoptaApiCustomerService, never()).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test
    public void testPanoptaCustomerDeletion() {
        when(panoptaApiCustomerService.getCustomer("gdtest_"+orionGuid)).thenReturn(fakePanoptaApiCustomerList);
        when(config.get(eq("panopta.api.partner.customer.key.prefix"))).thenReturn("gdtest_");
        when(responseStatusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);

        panoptaResource.deleteCustomer(orionGuid);

        verify(panoptaApiCustomerService, times(1)).getCustomer(eq("gdtest_" + orionGuid));
        verify(panoptaApiCustomerService, times(1)).deleteCustomer(eq("2hum-wpmt-vswt-2g3b"));
    }


}