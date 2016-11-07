package com.godaddy.vps4.web.hfs;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class HfsMockModule extends AbstractModule {

    @Override
    public void configure() {
        NetworkService netService = buildMockNetworkService();
        bind(NetworkService.class).toInstance(netService);
        VmService vmService = buildMockVmService();
        bind(VmService.class).toInstance(vmService);
        SysAdminService sysAdminService = Mockito.mock(SysAdminService.class);
        bind(SysAdminService.class).toInstance(sysAdminService);
    }

    private VmService buildMockVmService() {
        VmService vmService = Mockito.mock(VmService.class);
        Vm vm0 = new Vm();
        vm0.vmId = 0;
        vm0.status = "Live";
        Mockito.when(vmService.getVm(0)).thenReturn(vm0);
        VmAction completeDelAction = new VmAction();
        completeDelAction.vmActionId = 1111;
        completeDelAction.vmId = 0;
        completeDelAction.state = "COMPLETE";
        completeDelAction.tickNum = 1;
        Mockito.when(vmService.destroyVm(0)).thenReturn(completeDelAction);
        return vmService;
    }

    private NetworkService buildMockNetworkService() {
        Answer<AddressAction> answer = new Answer<AddressAction>() {
            // returns 3 in progress responses then a complete response
            private int timesCalled = 0;

            public AddressAction answer(InvocationOnMock invocation) throws Throwable {
                if (timesCalled < 3) {
                    timesCalled++;
                    return buildAddressAction(AddressAction.Status.IN_PROGRESS);
                }
                timesCalled = 0;
                return buildAddressAction(AddressAction.Status.COMPLETE);
            }
        };

        NetworkService netService = Mockito.mock(NetworkService.class);
        AddressAction newAction = buildAddressAction(AddressAction.Status.NEW);
        Mockito.when(netService.unbindIp(0)).thenReturn(newAction);
        Mockito.when(netService.releaseIp(0)).thenReturn(newAction);
        Mockito.when(netService.getAddressAction(12345, 54321)).thenAnswer(answer);
        return netService;
    }

    private AddressAction buildAddressAction(AddressAction.Status status) {
        AddressAction newAction = new AddressAction();
        newAction.status = status;
        newAction.addressId = 12345;
        newAction.addressActionId = 54321;
        return newAction;
    }
}