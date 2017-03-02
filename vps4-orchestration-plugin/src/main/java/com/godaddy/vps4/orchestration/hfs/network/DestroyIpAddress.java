package com.godaddy.vps4.orchestration.hfs.network;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.cpanel.WaitForCpanelAction;
import com.godaddy.vps4.orchestration.hfs.plesk.WaitForPleskAction;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.mailrelay.MailRelayUpdate;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class DestroyIpAddress implements Command<DestroyIpAddress.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DestroyIpAddress.class);

    final NetworkService networkService;
    final VirtualMachineService virtualMachineService;
    final CPanelService cpanelService;
    final PleskService pleskService;
    final MailRelayService mailRelayService;

    @Inject
    public DestroyIpAddress(NetworkService networkService, VirtualMachineService virtualMachineService, 
                            CPanelService cpanelService, PleskService pleskService, MailRelayService mailRelayService) {
        this.networkService = networkService;
        this.virtualMachineService = virtualMachineService;
        this.cpanelService = cpanelService;
        this.pleskService = pleskService;
        this.mailRelayService = mailRelayService;
    }

    @Override
    public Void execute(CommandContext context, DestroyIpAddress.Request request) {
        IpAddress address = request.ipAddress;
        logger.info("Deleting IP Adddress with addressId {}", address.ipAddressId);
        if(address.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)){
            releaseControlPanelLicense(context, request.vm, address.ipAddress);
            disableMailRelay(context, address.ipAddress);
        }
        context.execute(UnbindIp.class, address.ipAddressId);
        context.execute(ReleaseIp.class, address.ipAddressId);

        return null;
    }
    
    private void disableMailRelay(CommandContext context, String ipAddress) {
        MailRelayUpdate relayUpdate = new MailRelayUpdate();
        relayUpdate.quota = 0;
        context.execute("DisableMailRelay", ctx -> {
            mailRelayService.setRelayQuota(ipAddress, relayUpdate);
            return null;
        });
    }
    
    private void releaseControlPanelLicense(CommandContext context, VirtualMachine vm, String ipAddress) {
        if(virtualMachineService.virtualMachineHasCpanel(vm.vmId)){
            CPanelAction action = context.execute("Unlicense-Cpanel", ctx -> {
                // TODO put this back when the cpanel vertical service starts using our allocated IP
//                return cpanelService.licenseRelease(vm.hfsVmId, ipAddress);
                return cpanelService.licenseRelease(vm.hfsVmId, vm.primaryIpAddress.ipAddress);
            });
            context.execute(WaitForCpanelAction.class, action);
        }
        else if(virtualMachineService.virtualMachineHasPlesk(vm.vmId)){
            PleskAction action = context.execute("Unlicense-Plesk", ctx -> {
                return pleskService.licenseRelease(vm.hfsVmId);
            });
            context.execute(WaitForPleskAction.class, action);
        }
    }
    
    public static class Request{
        public IpAddress ipAddress;
        public VirtualMachine vm;
        
        public Request(IpAddress ipAddress, VirtualMachine vm){
            this.ipAddress = ipAddress;
            this.vm = vm;
        }
        
    }
}
