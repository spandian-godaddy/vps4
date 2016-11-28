package com.godaddy.vps4.hfs;

import org.mockito.Mockito;

import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.vm.CreateVMRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class HfsMockModule extends AbstractModule {

    @Override
    public void configure() {
        VmService vmService = buildMockVmService();
        bind(VmService.class).toInstance(vmService);
        CPanelService cpService = Mockito.mock(CPanelService.class);
        bind(CPanelService.class).toInstance(cpService);
        

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
        completeDelAction.state = VmAction.Status.COMPLETE;
        completeDelAction.tickNum = 1;
        Mockito.when(vmService.destroyVm(0)).thenReturn(completeDelAction);
        Mockito.when(vmService.createVm(Mockito.any(CreateVMRequest.class))).thenReturn(completeDelAction);
        return vmService;
    }

}