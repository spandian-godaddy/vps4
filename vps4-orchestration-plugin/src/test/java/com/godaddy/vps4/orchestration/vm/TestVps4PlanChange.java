package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Test;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.util.Monitoring;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.nodeping.NodePingCheck;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class TestVps4PlanChange {
    ActionService actionService = mock(ActionService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService networkService = mock(NetworkService.class);
    NodePingService nodePingService = mock(NodePingService.class);
    Monitoring monitoring = mock(Monitoring.class);

    Vps4PlanChange command = new Vps4PlanChange(virtualMachineService, null, networkService, monitoring);

    CommandContext context = mock(CommandContext.class);
    
    @SuppressWarnings("unchecked")
    @Test
    public void testChangePlanCallsUpdateVmManagedLevel() {
        runChangeManagedLevelToManagedTest();
        verify(context, times(1)).execute(eq("UpdateVmManagedLevel"), any(Function.class), eq(Void.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testChangePlanCallsDeleteMonitoringAccount() {
        runChangeManagedLevelToManagedTest();
        verify(context, times(1)).execute(contains("DeleteMonitoringAccount"), any(Function.class), eq(Void.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testChangePlanCallsCreateMonitoringCheckForVm() {
        runChangeManagedLevelToManagedTest();
        verify(context, times(1)).execute(contains("CreateMonitoringCheckForVm"), any(Function.class), eq(NodePingCheck.class));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testChangePlanCallsAddCheckIdToIp() {
        runChangeManagedLevelToManagedTest();
        verify(context, times(1)).execute(contains("AddCheckIdToIp"), any(Function.class), eq(Void.class));
    }
    
    @SuppressWarnings("unchecked")
    private void runChangeManagedLevelToManagedTest() {
        VirtualMachineCredit credit = new VirtualMachineCredit(UUID.randomUUID(), 10, 2, 0, "linux", "cpanel", 
                Instant.now(), "someShopper", AccountStatus.ACTIVE, null, UUID.randomUUID(), false);
        IpAddress primaryIpAddress = new IpAddress(0, credit.productId, "1.2.3.4", IpAddressType.PRIMARY, 123L, null, null);
        VirtualMachine vm = new VirtualMachine(credit.productId, 1234, credit.orionGuid, 1, null, "testVm", null, 
                primaryIpAddress, null, null, null, 0, null);
        Vps4PlanChange.Request request = new Vps4PlanChange.Request();
        request.vm = vm;
        request.credit = credit;
        
        when(context.execute(eq("UpdateVmManagedLevel"), any(Function.class), eq(Void.class))).thenReturn(null);
        when(context.execute(contains("DeleteMonitoringAccount"), any(Function.class), eq(Void.class))).thenReturn(null);
        NodePingCheck check = new NodePingCheck();
        check.checkId = 321;
        when(context.execute(contains("CreateMonitoringCheckForVm"), any(Function.class), eq(NodePingCheck.class))).thenReturn(check);
        when(context.execute(contains("AddCheckIdToIp"), any(Function.class), eq(Void.class))).thenReturn(null);
        when(monitoring.getAccountId(any(VirtualMachine.class))).thenReturn(123L);
        when(monitoring.getAccountId(2)).thenReturn(234L);
        
        try {
            command.execute(context, request);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
