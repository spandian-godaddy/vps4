package com.godaddy.vps4.web.dns;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.hfs.dns.HfsDnsAction;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.hfs.dns.RdnsRecords;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

@RunWith(value = MockitoJUnitRunner.class)
public class DnsResourceTest {
    private DnsResource dnsResource; // actual class under test
    private VmResource vmResource = mock(VmResource.class);
    private ActionService actionService = mock(ActionService.class);
    private CommandService commandService = mock(CommandService.class);
    private HfsDnsService dnsService = mock(HfsDnsService.class);
    private HfsDnsAction dnsAction = mock(HfsDnsAction.class);
    private ReverseDnsLookup reverseDnsLookup = mock(ReverseDnsLookup.class);

    private Vm hfsVm = new Vm();
    private long hfsVmId = 777;
    private String fakeReverseDnsName = "fake_reverse_dns_name";
    private UUID orionGuid = UUID.randomUUID();
    private UUID vmId = UUID.randomUUID();
    private GDUser user = GDUserMock.createShopper();
    private Injector injector = Guice.createInjector(binder -> binder.bind(ObjectMapperProvider.class));

    private VirtualMachine testVm;
    private RdnsRecords rdnsRecords;
    private DnsResource.ReverseDnsNameRequest request = new DnsResource.ReverseDnsNameRequest();

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        ObjectMapperProvider objectMapperProvider = injector.getProvider(ObjectMapperProvider.class).get();
        Action testAction = new Action(123L, vmId, ActionType.CREATE_REVERSE_DNS_NAME_RECORD, null, null, null,
                                       ActionStatus.COMPLETE, Instant.now(), Instant.now(), null, UUID.randomUUID(),
                                       null);
        when(actionService.getAction(anyLong())).thenReturn(testAction);
        when(actionService.createAction(eq(vmId), eq(ActionType.CREATE_REVERSE_DNS_NAME_RECORD), anyString(), anyString()))
                .thenReturn(testAction.id);
        when(commandService.executeCommand(anyObject())).thenReturn(new CommandState());
        rdnsRecords = createFakeRdnsRecords();

        dnsResource =
                new DnsResource(dnsService, vmResource, actionService, commandService, user, objectMapperProvider,
                                reverseDnsLookup);
        request.reverseDnsName = fakeReverseDnsName;

    }

    private void createDedTestVm() {
        createTestVm(ServerType.Type.DEDICATED);
    }

    private void createTestVm(ServerType.Type serverType) {
        ServerSpec testSpec = new ServerSpec();
        testSpec.serverType = new ServerType();
        testSpec.serverType.serverType = serverType;

        testVm = new VirtualMachine();
        testVm.vmId = vmId;
        testVm.hfsVmId = hfsVmId;
        testVm.orionGuid = orionGuid;
        testVm.canceled = Instant.now().plus(7, ChronoUnit.DAYS);
        testVm.validUntil = Instant.MAX;
        testVm.spec = testSpec;
        testVm.primaryIpAddress =
                new IpAddress(1111, 1111, vmId, "1.2.3.4", IpAddress.IpAddressType.PRIMARY, null, Instant.now(),
                              Instant.now().plus(24, ChronoUnit.HOURS));
        when(vmResource.getVm(testVm.vmId)).thenReturn(testVm);
    }

    private RdnsRecords createFakeRdnsRecords() {
        rdnsRecords = new RdnsRecords();
        rdnsRecords.results = new RdnsRecords.Results[]{};
        return rdnsRecords;
    }

    @Test
    public void invokesGet() {
        createDedTestVm();
        when(dnsService.getReverseDnsName(anyLong())).thenReturn(rdnsRecords);

        dnsResource.getReverseDnsName(testVm.vmId, testVm.primaryIpAddress.ipAddress);

        verify(dnsService, times(1)).getReverseDnsName(eq(hfsVmId));
    }

    @Test
    public void invokesOrchestrationCommandToCreateReverseDnsName() {
        createDedTestVm();
        hfsVm.status = "ACTIVE";
        when(vmResource.getVmFromVmVertical(anyLong())).thenReturn(hfsVm);
        doNothing().when(reverseDnsLookup).validateReverseDnsName(anyString(), eq("1.2.3.4"));
        when(dnsService.createDnsPtrRecord(anyLong(), anyString())).thenReturn(dnsAction);

        dnsResource.createDnsPtrRecord(testVm.vmId, testVm.primaryIpAddress.ipAddress, request);

        ArgumentCaptor<CommandGroupSpec> argument = ArgumentCaptor.forClass(CommandGroupSpec.class);
        verify(commandService, times(1)).executeCommand(argument.capture());
        Assert.assertEquals("Vps4CreateDnsPtrRecord", argument.getValue().commands.get(0).command);
        verify(actionService, times(1)).getAction(anyLong());
    }

    @Test(expected = Vps4Exception.class)
    public void doesNotAllowMismatchedVmIpAddressPairToCreateReverseDnsName() {
        createDedTestVm();
        hfsVm.status = "ACTIVE";
        when(vmResource.getVmFromVmVertical(anyLong())).thenReturn(hfsVm);
        doNothing().when(reverseDnsLookup).validateReverseDnsName(anyString(), eq("1.2.3.4"));
        when(dnsService.createDnsPtrRecord(anyLong(), anyString())).thenReturn(dnsAction);

        try {
            dnsResource.createDnsPtrRecord(testVm.vmId, "mismatched_ip_address", request);
        } catch (Vps4Exception vex) {
            assert (vex.getMessage().equalsIgnoreCase(
                    "Ip address provided does not match the primary ip address for the vm."));
            assert (vex.getId().equalsIgnoreCase("IP_ADDRESS_NOT_ASSOCIATED_WITH_VM"));
            throw vex;
        }
        fail("Expected Vps4Exception to be thrown for ip address mismatch with vm");
    }

    @Test
    public void performsReverseDnsNameLookup() {
        createDedTestVm();
        hfsVm.status = "ACTIVE";
        when(vmResource.getVmFromVmVertical(anyLong())).thenReturn(hfsVm);
        doNothing().when(reverseDnsLookup).validateReverseDnsName(anyString(), eq("1.2.3.4"));
        when(dnsService.createDnsPtrRecord(anyLong(), anyString())).thenReturn(dnsAction);

        dnsResource.createDnsPtrRecord(testVm.vmId, "1.2.3.4", request);
        verify(reverseDnsLookup, times(1)).validateReverseDnsName(eq(fakeReverseDnsName), eq("1.2.3.4"));
    }

    @Test(expected = Vps4Exception.class)
    public void throwsExceptionIfNameLookupFailsOnIpAddress() {
        createDedTestVm();
        hfsVm.status = "ACTIVE";
        when(vmResource.getVmFromVmVertical(anyLong())).thenReturn(hfsVm);
        doThrow(new Vps4Exception("IP_ADDRESS_LOOKUP_FAILED", "fakeException")).when(reverseDnsLookup)
                                                                               .validateReverseDnsName(anyString(),
                                                                                                       eq("1.2.3.4"));
        when(dnsService.createDnsPtrRecord(anyLong(), anyString())).thenReturn(dnsAction);

        try {
            dnsResource.createDnsPtrRecord(testVm.vmId, "1.2.3.4", request);
        } catch (Vps4Exception vex) {
            assert (vex.getId().equalsIgnoreCase("IP_ADDRESS_LOOKUP_FAILED"));
            throw vex;
        }
    }
}
