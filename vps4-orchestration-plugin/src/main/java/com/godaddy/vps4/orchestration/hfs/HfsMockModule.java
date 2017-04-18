package com.godaddy.vps4.orchestration.hfs;


import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayHistory;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.CreateVMRequest;
import gdg.hfs.vhfs.vm.CreateVMWithFlavorRequest;
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
        SysAdminService sysAdminService = buildSysAdminService();
        bind(SysAdminService.class).toInstance(sysAdminService);
        CPanelService cpService = Mockito.mock(CPanelService.class);
        bind(CPanelService.class).toInstance(cpService);
        MailRelayService mailRelayService = buildMailRelayService();
        bind(MailRelayService.class).toInstance(mailRelayService);
        PleskService pleskService = buildPleskService();
        bind(PleskService.class).toInstance(pleskService);
        
    }
    
    private PleskService buildPleskService() {
        PleskAction completeAction = new PleskAction();
        completeAction.status = PleskAction.Status.COMPLETE;
        PleskService pleskService = Mockito.mock(PleskService.class);
        Mockito.when(pleskService.imageConfig(Mockito.anyLong(),  Mockito.anyString(), Mockito.anyString())).thenReturn(completeAction);
        Mockito.when(pleskService.adminPassUpdate(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
        return pleskService;
    }

    private MailRelayService buildMailRelayService() {
        MailRelayService mailRelayService = Mockito.mock(MailRelayService.class);

        MailRelay mailRelayTarget = new MailRelay();
        mailRelayTarget.quota = 5000;
        
        MailRelayHistory singleHistory = new MailRelayHistory();
        List<MailRelayHistory> history = new ArrayList<>();
        history.add(singleHistory);

        Mockito.when(mailRelayService.setRelayQuota(Mockito.anyString(), Mockito.anyObject())).thenReturn(mailRelayTarget);
        Mockito.when(mailRelayService.getMailRelay(Mockito.anyString())).thenReturn(mailRelayTarget);
        Mockito.when(mailRelayService.getRelayHistory(Mockito.anyString())).thenReturn(history);

        return mailRelayService;
    }

    private SysAdminService buildSysAdminService(){
       SysAdminService sysAdminService = Mockito.mock(SysAdminService.class);
       SysAdminAction completeAction = new SysAdminAction();
       completeAction.status = SysAdminAction.Status.COMPLETE;

       SysAdminAction errorAction = new SysAdminAction();
       errorAction.status = SysAdminAction.Status.FAILED;

       Mockito.when(sysAdminService.enableAdmin(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
       Mockito.when(sysAdminService.disableAdmin(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);
       Mockito.when(sysAdminService.changePassword(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())).thenReturn(completeAction);
       Mockito.when(sysAdminService.changeHostname(Mockito.anyLong(), Mockito.anyString(), Mockito.any())).thenReturn(completeAction);
       Mockito.when(sysAdminService.getSysAdminAction(Mockito.anyLong())).thenReturn(completeAction);
        Mockito.when(sysAdminService.configureMTA(Mockito.anyLong(), Mockito.anyString())).thenReturn(completeAction);

       return sysAdminService;

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
        Mockito.when(vmService.createVmWithFlavor(Mockito.any(CreateVMWithFlavorRequest.class))).thenReturn(completeDelAction);
        Mockito.when(vmService.getVmAction(Mockito.anyLong(),Mockito.anyLong())).thenReturn(completeDelAction);
        Mockito.when(vmService.destroyVm(0)).thenReturn(completeDelAction);
        Mockito.when(vmService.createVm(Mockito.any(CreateVMRequest.class))).thenReturn(completeDelAction);
        Mockito.when(vmService.startVm(Mockito.anyLong())).thenReturn(completeDelAction);
        Mockito.when(vmService.stopVm(Mockito.anyLong())).thenReturn(completeDelAction);
        Mockito.when(vmService.createVmWithFlavor(Mockito.any(CreateVMWithFlavorRequest.class))).thenReturn(completeDelAction);
        Mockito.when(vmService.getVmAction(Mockito.anyLong(), Mockito.anyLong())).thenReturn(completeDelAction);
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
        Mockito.when(netService.acquireIp(Mockito.anyString(), Mockito.anyString())).thenReturn(newAction);
        Mockito.when(netService.bindIp(12345,0)).thenReturn(newAction);
        Mockito.when(netService.getAddressAction(12345, 54321)).thenAnswer(answer);
        Mockito.when(netService.getAddress(12345)).thenReturn(buildIpAddress());
        return netService;
    }

    private IpAddress buildIpAddress(){
        IpAddress ip = new IpAddress();
        ip.address = "2.23.123.111";
        ip.status = IpAddress.Status.UNBOUND;
        ip.addressId = 12345;
        return ip;
    }

    private AddressAction buildAddressAction(AddressAction.Status status) {
        AddressAction newAction = new AddressAction();
        newAction.status = status;
        newAction.addressId = 12345;
        newAction.addressActionId = 54321;
        return newAction;
    }
}