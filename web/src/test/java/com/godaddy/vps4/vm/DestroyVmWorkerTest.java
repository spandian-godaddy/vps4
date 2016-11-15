package com.godaddy.vps4.vm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.web.vm.DestroyVmWorker;
import com.godaddy.vps4.web.vm.VmResource.DestroyVmAction;
import com.google.inject.Injector;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.AddressAction.Status;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class DestroyVmWorkerTest {

    private static com.godaddy.vps4.network.NetworkService vps4NetworkService;
    private static NetworkService hfsNetworkSerivce;
    private static VirtualMachineService virtualMachineService;

    private static VmService vmService;
    private DestroyVmAction action;

    private List<IpAddress> addresses;
    private static ExecutorService threadPool;

    static Injector injector;
    static DataSource dataSource;

    @Before
    public void setupTest() {
        vps4NetworkService = Mockito.mock(com.godaddy.vps4.network.NetworkService.class);
        hfsNetworkSerivce = Mockito.mock(NetworkService.class);
        vmService = Mockito.mock(VmService.class);
        virtualMachineService = Mockito.mock(VirtualMachineService.class);
        threadPool = Executors.newCachedThreadPool();

        action = new DestroyVmAction();
        action.virtualMachine = new VirtualMachine(1, UUID.randomUUID(), 1, null, "testDestroyVmServerName", "centos-7", Instant.now(),
                Instant.MAX);

        addresses = new ArrayList<IpAddress>();
        addresses
                .add(new IpAddress(123, action.virtualMachine.projectId, "192.168.1.1", IpAddressType.PRIMARY, Instant.now(), Instant.MAX));
        addresses.add(
                new IpAddress(124, action.virtualMachine.projectId, "192.168.1.2", IpAddressType.SECONDARY, Instant.now(), Instant.MAX));
    }

    @Test
    public void testDestroyVm() {
        when(vps4NetworkService.getVmIpAddresses(action.virtualMachine.vmId)).thenReturn(addresses);

        when(hfsNetworkSerivce.unbindIp(anyLong())).thenAnswer(getAddressActionAnswer(Status.IN_PROGRESS));
        when(hfsNetworkSerivce.releaseIp(anyLong())).thenAnswer(getAddressActionAnswer(Status.IN_PROGRESS));
        when(hfsNetworkSerivce.getAddressAction(anyLong(), anyLong())).thenAnswer(getAddressActionAnswer(Status.COMPLETE));

        when(vmService.destroyVm(action.virtualMachine.vmId)).thenAnswer(getVmActionAnswer(VmAction.Status.IN_PROGRESS));
        when(vmService.getVmAction(action.virtualMachine.vmId, 1)).thenAnswer(getVmActionAnswer(VmAction.Status.COMPLETE));

        DestroyVmWorker worker = new DestroyVmWorker(action, vmService, hfsNetworkSerivce, vps4NetworkService, virtualMachineService,
                threadPool);
        worker.run();

        assertThat(action.status, is(equalTo(ActionStatus.COMPLETE)));
        addresses.stream().forEach(a -> verify(hfsNetworkSerivce, times(1)).unbindIp(a.ipAddressId));
        addresses.stream().forEach(a -> verify(hfsNetworkSerivce, atMost(1)).releaseIp(a.ipAddressId));
        addresses.stream().forEach(a -> verify(vps4NetworkService, atMost(1)).destroyIpAddress(a.ipAddressId));

        verify(vmService, times(1)).destroyVm(action.virtualMachine.vmId);
        verify(virtualMachineService, times(1)).destroyVirtualMachine(action.virtualMachine.vmId);
    }

    private Answer<VmAction> getVmActionAnswer(VmAction.Status state) {
        return new Answer<VmAction>() {

            @Override
            public VmAction answer(InvocationOnMock invocation) throws Throwable {
                VmAction vmAction = new VmAction();
                vmAction.vmActionId = 1;
                vmAction.state = state;
                return vmAction;

            }
        };
    }

    private Answer<AddressAction> getAddressActionAnswer(Status status) {
        return new Answer<AddressAction>() {

            @Override
            public AddressAction answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                AddressAction tempAddressAction = new AddressAction();
                tempAddressAction.addressActionId = 1;
                tempAddressAction.addressId = (long) args[0];
                tempAddressAction.status = status;
                return tempAddressAction;

            }
        };
    }

    @Test
    public void testDestroyVmUnbindFails() {
        when(vps4NetworkService.getVmIpAddresses(action.virtualMachine.vmId)).thenReturn(addresses);

        when(hfsNetworkSerivce.unbindIp(anyLong())).thenAnswer(getAddressActionAnswer(Status.IN_PROGRESS));
        when(hfsNetworkSerivce.getAddressAction(anyLong(), anyLong())).thenAnswer(getAddressActionAnswer(Status.FAILED));

        DestroyVmWorker worker = new DestroyVmWorker(action, vmService, hfsNetworkSerivce, vps4NetworkService, virtualMachineService,
                threadPool);
        worker.run();

        assertThat(action.status, is(equalTo(ActionStatus.ERROR)));
        addresses.stream().forEach(a -> verify(hfsNetworkSerivce, times(1)).unbindIp(a.ipAddressId));
        addresses.stream().forEach(a -> verify(hfsNetworkSerivce, times(0)).releaseIp(a.ipAddressId));
        addresses.stream().forEach(a -> verify(vps4NetworkService, times(0)).destroyIpAddress(a.ipAddressId));

        verify(vmService, times(0)).destroyVm(action.virtualMachine.vmId);
        verify(virtualMachineService, times(0)).destroyVirtualMachine(action.virtualMachine.vmId);
    }

    @Test
    public void testDestroyVmDestroyVmFails() {
        when(vps4NetworkService.getVmIpAddresses(action.virtualMachine.vmId)).thenReturn(addresses);

        when(hfsNetworkSerivce.unbindIp(anyLong())).thenAnswer(getAddressActionAnswer(Status.IN_PROGRESS));
        when(hfsNetworkSerivce.releaseIp(anyLong())).thenAnswer(getAddressActionAnswer(Status.IN_PROGRESS));
        when(hfsNetworkSerivce.getAddressAction(anyLong(), anyLong())).thenAnswer(getAddressActionAnswer(Status.COMPLETE));

        when(vmService.destroyVm(action.virtualMachine.vmId)).thenAnswer(getVmActionAnswer(VmAction.Status.IN_PROGRESS));
        when(vmService.getVmAction(action.virtualMachine.vmId, 1)).thenAnswer(getVmActionAnswer(VmAction.Status.FAILED));

        DestroyVmWorker worker = new DestroyVmWorker(action, vmService, hfsNetworkSerivce, vps4NetworkService, virtualMachineService,
                threadPool);
        worker.run();

        assertThat(action.status, is(equalTo(ActionStatus.ERROR)));
        addresses.stream().forEach(a -> verify(hfsNetworkSerivce, times(1)).unbindIp(a.ipAddressId));
        addresses.stream().forEach(a -> verify(hfsNetworkSerivce, times(1)).releaseIp(a.ipAddressId));
        addresses.stream().forEach(a -> verify(vps4NetworkService, atMost(1)).destroyIpAddress(a.ipAddressId));

        verify(vmService, times(1)).destroyVm(action.virtualMachine.vmId);
        verify(virtualMachineService, times(0)).destroyVirtualMachine(action.virtualMachine.vmId);
    }

}
