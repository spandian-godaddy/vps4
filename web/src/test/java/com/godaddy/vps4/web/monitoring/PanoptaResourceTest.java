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
import java.time.Instant;
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
import com.godaddy.vps4.panopta.DefaultPanoptaService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerList;
import com.godaddy.vps4.panopta.PanoptaApiCustomerRequest;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServers;
import com.godaddy.vps4.panopta.PanoptaService;
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
    private PanoptaApiCustomerService panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
    private PanoptaApiServerService panoptaApiServerService = mock(PanoptaApiServerService.class);
    private CreditService creditService = mock(CreditService.class);
    private DataCenterService dataCenterService = mock(DataCenterService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private ServerSpec serverSpec = mock(ServerSpec.class);
    private PanoptaService panoptaService = mock(PanoptaService.class);
    private PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    private Response.StatusType responseStatusType = mock(Response.StatusType.class);
    private GDUser user = GDUserMock.createShopper();

    private VirtualMachineCredit credit;
    private VirtualMachineCredit otherCredit;
    private VirtualMachine virtualMachine;
    private UUID vmId = UUID.randomUUID();
    private UUID orionGuid = UUID.randomUUID();
    PanoptaResource.CreateCustomerRequest createCustomerRequest;
    @Inject
    private PanoptaApiCustomerList fakePanoptaApiCustomerList;
    @Inject
    private PanoptaServers fakePanoptaApiServers;
    @Inject
    private ObjectMapper objectMapper;

    private Injector injector1 = Guice.createInjector(
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
                    bind(VmResource.class).toInstance(vmResource);
                    bind(PanoptaService.class).to(DefaultPanoptaService.class);
                    bind(PanoptaDataService.class).toInstance(panoptaDataService);
                    bind(GDUser.class).toInstance(user);
                }
            });
    private Injector injector2 = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Config.class).toInstance(config);
                    bind(CreditService.class).toInstance(creditService);
                    bind(VirtualMachineService.class).toInstance(virtualMachineService);
                    bind(VmResource.class).toInstance(vmResource);
                    bind(PanoptaService.class).toInstance(panoptaService);
                    bind(GDUser.class).toInstance(user);
                }
            });

    private PanoptaResource panoptaResource1 = injector1.getInstance(PanoptaResource.class);
    private PanoptaResource panoptaResource2 = injector2.getInstance(PanoptaResource.class);

    @Before
    public void setupTest() {
        injector1.injectMembers(this);
        injector2.injectMembers(this);
        credit = createCredit(user.getShopperId(), AccountStatus.ACTIVE, "10");
        otherCredit = createCredit("SomeOtherShopper", AccountStatus.ACTIVE, "10");
        virtualMachine =
                new VirtualMachine(vmId, 123L, orionGuid, 321L, new ServerSpec(), "TestVm", null, null,
                                   Instant.now(), null, Instant.MAX, null, 0, UUID.randomUUID());
        when(vmResource.getVm(vmId)).thenReturn(virtualMachine);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(orionGuid)).thenReturn(credit);
        createCustomerRequest = new PanoptaResource.CreateCustomerRequest();
        try {
            fakePanoptaApiCustomerList = objectMapper.readValue(mockedupCustomerList(), PanoptaApiCustomerList.class);
            fakePanoptaApiServers = objectMapper.readValue(mockedupServer(), PanoptaServers.class);
        } catch (IOException ex) {
            fail("Could not setup test. " + ex.getMessage());
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

    private String mockedupCustomerList() {
        return "{\n" +
                "  \"customer_list\": [\n" +
                "    {\n" +
                "      \"customer_key\": \"2hum-wpmt-vswt-2g3b\",\n" +
                "      \"email_address\": \"abhoite@godaddy.com\",\n" +
                "      \"name\": \"Godaddy VPS4 POC\",\n" +
                "      \"package\": \"godaddy.fully_managed\",\n" +
                "      \"partner_customer_key\": \"gdtest_" + vmId + "\",\n" +
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

    private String mockedupServer() {
        return "{\n" +
                "  \"meta\": {\n" +
                "    \"limit\": 50,\n" +
                "    \"next\": null,\n" +
                "    \"offset\": 0,\n" +
                "    \"previous\": null,\n" +
                "    \"total_count\": 2\n" +
                "  },\n" +
                "  \"server_list\": [\n" +
                "    {\n" +
                "      \"additional_fqdns\": [\n" +
                "        \"169.254.254.28\",\n" +
                "        \"64.202.187.12\"\n" +
                "      ],\n" +
                "      \"agent_heartbeat_delay\": 10,\n" +
                "      \"agent_heartbeat_enabled\": true,\n" +
                "      \"agent_heartbeat_notification_schedule\": \"https://api2.panopta" +
                ".com/v2/notification_schedule/-1\",\n" +
                "      \"agent_installed\": true,\n" +
                "      \"agent_last_sync_time\": \"2019-07-30 21:44:57\",\n" +
                "      \"agent_version\": \"19.12.5\",\n" +
                "      \"attributes\": [\n" +
                "        {\n" +
                "          \"server_attribute_type\": \"https://api2.panopta.com/v2/server_attribute_type/315\",\n" +
                "          \"url\": \"https://api2.panopta.com/v2/server/1105606/server_attribute/803531\",\n" +
                "          \"value\": \"agent\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"server_attribute_type\": \"https://api2.panopta.com/v2/server_attribute_type/290\",\n" +
                "          \"url\": \"https://api2.panopta.com/v2/server/1105606/server_attribute/803532\",\n" +
                "          \"value\": \"Windows\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"server_attribute_type\": \"https://api2.panopta.com/v2/server_attribute_type/294\",\n" +
                "          \"url\": \"https://api2.panopta.com/v2/server/1105606/server_attribute/803533\",\n" +
                "          \"value\": \"1\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"server_attribute_type\": \"https://api2.panopta.com/v2/server_attribute_type/293\",\n" +
                "          \"url\": \"https://api2.panopta.com/v2/server/1105606/server_attribute/803534\",\n" +
                "          \"value\": \"Microsoft Windows NT 6.2.9200.0\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"server_attribute_type\": \"https://api2.panopta.com/v2/server_attribute_type/295\",\n" +
                "          \"url\": \"https://api2.panopta.com/v2/server/1105606/server_attribute/803535\",\n" +
                "          \"value\": \"x64\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"auxiliary_notification\": {\n" +
                "        \"agent_heartbeats\": [],\n" +
                "        \"agent_thresholds\": [],\n" +
                "        \"network_outages\": [],\n" +
                "        \"snmp_heartbeats\": [],\n" +
                "        \"snmp_thresholds\": [],\n" +
                "        \"wmi_heartbeats\": [],\n" +
                "        \"wmi_thresholds\": []\n" +
                "      },\n" +
                "      \"auxiliary_notification_schedules\": [],\n" +
                "      \"billing_type\": \"advanced\",\n" +
                "      \"countermeasures_enabled\": false,\n" +
                "      \"created\": \"Fri, 24 May 2019 23:22:29 -0000\",\n" +
                "      \"current_outages\": [],\n" +
                "      \"current_state\": \"up\",\n" +
                "      \"deleted\": null,\n" +
                "      \"description\": \"\",\n" +
                "      \"device_type\": \"server\",\n" +
                "      \"fqdn\": \"s64-202-187-12\",\n" +
                "      \"name\": \"s64-202-187-12\",\n" +
                "      \"notification_schedule\": \"https://api2.panopta.com/v2/notification_schedule/184642\",\n" +
                "      \"notify_agent_heartbeat_failure\": true,\n" +
                "      \"parent_server\": null,\n" +
                "      \"partner_server_id\": null,\n" +
                "      \"primary_monitoring_node\": \"https://api2.panopta.com/v2/monitoring_node/51\",\n" +
                "      \"server_group\": \"https://api2.panopta.com/v2/server_group/346861\",\n" +
                "      \"server_key\": \"d3cn-thrm-xovb-ona8\",\n" +
                "      \"server_template\": [],\n" +
                "      \"snmp_heartbeat_delay\": 10,\n" +
                "      \"snmp_heartbeat_enabled\": false,\n" +
                "      \"snmp_heartbeat_notification_schedule\": null,\n" +
                "      \"snmpcredential\": null,\n" +
                "      \"status\": \"active\",\n" +
                "      \"tags\": [],\n" +
                "      \"template_ignore_agent_heartbeat\": false,\n" +
                "      \"template_ignore_snmp_heartbeat\": false,\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server/1105606\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
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
        when(panoptaApiCustomerService.getCustomer(anyString())).thenReturn(fakePanoptaApiCustomerList);
        createCustomerRequest.vmId = vmId.toString();

        panoptaResource1.createCustomer(createCustomerRequest);

        verify(panoptaApiCustomerService, times(1)).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throwsExceptionIfCreditIsForDedicatedServer() {
        when(creditService.getVirtualMachineCredit(eq(orionGuid))).thenReturn(credit);
        when(virtualMachineService.getSpec(anyInt())).thenReturn(serverSpec);
        when(serverSpec.isVirtualMachine()).thenReturn(false);
        createCustomerRequest.vmId = vmId.toString();

        panoptaResource1.createCustomer(createCustomerRequest);

        verify(panoptaApiCustomerService, never()).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test
    public void testCustomerDeletion() {
        String customerKey = "2hum-wpmt-vswt-2g3b";
        PanoptaDetail panoptaDetails = new PanoptaDetail(42L, vmId, "partnerCustomerKey", customerKey,
                23L, "serverKey", Instant.now(), null);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetails);
        panoptaResource1.deleteCustomer(vmId);
        verify(panoptaApiCustomerService, times(1)).deleteCustomer(customerKey);
    }

    @Test
    public void testGetServer() {
        when(panoptaApiServerService.getPanoptaServers("gdtest_" + vmId)).thenReturn(fakePanoptaApiServers);
        when(config.get(eq("panopta.api.partner.customer.key.prefix"))).thenReturn("gdtest_");
        when(responseStatusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);

        panoptaResource1.getServer(vmId);

        verify(panoptaApiServerService, times(1)).getPanoptaServers(eq("gdtest_" + vmId));
    }
}