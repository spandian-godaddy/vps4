package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.plesk.WaitForPleskAction;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;

public class Vps4DestroyIpAddress implements Command<Vps4DestroyIpAddress.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyIpAddress.class);

    final NetworkService networkService;
    final VirtualMachineService virtualMachineService;
    final CPanelService cpanelService;
    final PleskService pleskService;
    final MailRelayService mailRelayService;

    @Inject
    public Vps4DestroyIpAddress(NetworkService networkService, VirtualMachineService virtualMachineService,
            CPanelService cpanelService, PleskService pleskService, MailRelayService mailRelayService) {
        this.networkService = networkService;
        this.virtualMachineService = virtualMachineService;
        this.cpanelService = cpanelService;
        this.pleskService = pleskService;
        this.mailRelayService = mailRelayService;
    }

    @Override
    public Void execute(CommandContext context, Vps4DestroyIpAddress.Request request) {
        IpAddress address = request.ipAddress;
        logger.info("Deleting IP Adddress with addressId {}", address.ipAddressId);
        if (address.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)) {
            disableMailRelay(context, address.ipAddress);
        }

        UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
        unbindIpRequest.addressId = address.ipAddressId;
        unbindIpRequest.forceIfVmInaccessible = request.forceIfVmInaccessible;

        context.execute(UnbindIp.class, unbindIpRequest);
        context.execute(ReleaseIp.class, address.ipAddressId);

        return null;
    }

    private void disableMailRelay(CommandContext context, String ipAddress) {
        SetMailRelayQuota.Request hfsRequest = new SetMailRelayQuota.Request();
        hfsRequest.ipAddress = ipAddress;
        hfsRequest.mailRelayQuota = 0;
        context.execute(SetMailRelayQuota.class, hfsRequest);
    }

    public static class Request {
        public IpAddress ipAddress;
        public VirtualMachine vm;
        public boolean forceIfVmInaccessible;

        public Request(IpAddress ipAddress, VirtualMachine vm, boolean forceIfVmInaccessible) {
            this.ipAddress = ipAddress;
            this.vm = vm;
            this.forceIfVmInaccessible = forceIfVmInaccessible;
        }

    }
}
