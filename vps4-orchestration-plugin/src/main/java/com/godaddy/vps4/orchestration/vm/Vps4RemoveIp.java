package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveIp implements Command<IpAddress, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveIp.class);

    final MailRelayService mailRelayService;

    @Inject
    public Vps4RemoveIp(MailRelayService mailRelayService) {
        this.mailRelayService = mailRelayService;
    }

    @Override
    public Void execute(CommandContext context, IpAddress address) {
        logger.info("Deleting IP Adddress with addressId {}", address.ipAddressId);
        if (address.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)) {
            disableMailRelay(context, address.ipAddress);
        }

        UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
        unbindIpRequest.addressId = address.ipAddressId;
        unbindIpRequest.forceIfVmInaccessible = true;

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
}
