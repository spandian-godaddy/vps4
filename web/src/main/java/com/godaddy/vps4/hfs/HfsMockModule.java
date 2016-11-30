package com.godaddy.vps4.hfs;

import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.vm.CreateVMRequest;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import gdg.hfs.request.CompleteResponse;
import gdg.hfs.vhfs.cpanel.CPanelAction;

public class HfsMockModule extends AbstractModule {

    private static final long actionId = 123;
    private static final CPanelAction reqAccessRet;
    private static final CPanelAction getActionRet;

    static final String accessHash = "b32b85d55e3d94408b78f729455e0076"
          + "930065e534418a463322bd966edc5a1e"
          + "bc7b61ddfbeb2958a6f276c56fd90ba3"
          + "0ce8d33ecbcab48d4500a32d465c98ac"
          + "5d8d9ce03c82702b0fb64d5813753284"
          + "b66087dac6937ff62f2074b71c14b173"
          + "e9dc90397b2c548dbe0cf16eecc114da"
          + "e25a6d54e28c64195f0141881b012594"
          + "107764759dbbb30af52b26bbeb1f4a58"
          + "15401bdef541893243cb04dee1fc4f0a"
          + "ebae588f6ce6dbe6ed160e644a5da3c4"
          + "edff957b4e79cc0914ab952e7dd665a8"
          + "f09abe02e9d32058713661f35a974716"
          + "830b108f2372305ceba0e8342e1b11fc"
          + "61f10dbf9b195697647c7d4e8afafe72"
          + "45a74dffb07250fc213f80cdd6dde280"
          + "b9b81a2c6637215b98a51601a32c5d8d"
          + "6382f11e086a3f0f9bd5a999051729be"
          + "f661e8a731ec34603261d260101d9451"
          + "83c459cb9e1681e040e414006ca5bc99"
          + "b7354ff60959bcea14ac702dd4db2099"
          + "002bfc5f7550811bf2a639c1115d75ad"
          + "153108ac3b2fde2d266334037975f8d4"
          + "e083f7a2929d40bb7b9abcd1b5405340"
          + "fe93a9521bf49bef92937bc3121137f2"
          + "0a022fc590d01c1624e8200645ec0856"
          + "41e1f0a3dc939f8b42cc2d94ba02b4f0"
          + "593c8fe788d2eeca1be2580935a35da8"
          + "a209ef0bf98300d4f2f1c610255b8852"
          + "ecf8786a830798f564102fdd6fd0faec";

    static {
        reqAccessRet = new CPanelAction();
        reqAccessRet.status = CPanelAction.Status.IN_PROGRESS;
        reqAccessRet.actionId = actionId;

        getActionRet = new CPanelAction();
        getActionRet.status = CPanelAction.Status.COMPLETE;
        getActionRet.actionId = actionId;
        getActionRet.responsePayload = String.format("{\"cphash\":\"%s\",\"success\":true,\"error\":null}", accessHash);
    }

    @Override
    public void configure() {
        VmService vmService = buildMockVmService();
        bind(VmService.class).toInstance(vmService);
    }
    
    private VmService buildMockVmService() {
        VmService vmService = Mockito.mock(VmService.class);
        Vm vm0 = new Vm();
        vm0.vmId = 0;
        vm0.status = "Live";
        Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(vm0);
        VmAction completeDelAction = new VmAction();
        completeDelAction.vmActionId = 1111;
        completeDelAction.vmId = 0;
        completeDelAction.state = VmAction.Status.COMPLETE;
        completeDelAction.tickNum = 1;
        Mockito.when(vmService.destroyVm(0)).thenReturn(completeDelAction);
        Mockito.when(vmService.createVm(Mockito.any(CreateVMRequest.class))).thenReturn(completeDelAction);
        return vmService;
    }

    @Provides
    public CPanelService provideCPanelService() {
        /*
        CPanelService cpService = Mockito.mock(CPanelService.class);
        Mockito.when(cpService.requestAccess(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())).thenReturn(reqAccessRet);
        Mockito.when(cpService.getAction(reqAccessRet.actionId)).thenReturn(getActionRet);
        return cpService;
        */

        return new CPanelService() {

            @Override
            public CPanelAction getAction(long arg0) {
                return getActionRet;
            }

            @Override
            public CPanelAction requestAccess(long arg0, String arg1, String arg2) {
                return reqAccessRet;
            }

            @Override
            public CPanelAction imagePrep(long arg0) {
                // NOTE: do nothing, Implement when needed
                return null;
            }

            @Override
            public CPanelAction licenseActivate(long arg0, String arg1) {
                // NOTE: do nothing, Implement when needed
                return null;
            }

            @Override
            public CPanelAction licenseRefresh(long arg0) {
                // NOTE: do nothing, Implement when needed
                return null;
            }

            @Override
            public CPanelAction licenseRelease(long arg0, String arg1) {
                // NOTE: do nothing, Implement when needed
                return null;
            }

            @Override
            public CPanelAction licenseUpdateIP(long arg0, String arg1, String arg2) {
                // NOTE: do nothing, Implement when needed
                return null;
            }

            @Override
            public void onComplete(CompleteResponse arg0) {
                // NOTE: do nothing, Implement when needed
            }

            @Override
            public CPanelAction requestSiteList(long arg0, String arg1, String arg2) {
                // NOTE: do nothing, Implement when needed
                return null;
            }

            @Override
            public CPanelAction imageConfig(long arg0, String arg1) {
                // NOTE: do nothing, Implement when needed
                return null;
            }
        };
    }
}
