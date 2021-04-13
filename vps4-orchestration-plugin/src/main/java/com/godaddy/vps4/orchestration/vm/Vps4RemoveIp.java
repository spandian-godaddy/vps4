package com.godaddy.vps4.orchestration.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveIp implements Command<IpAddress, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveIp.class);

    @Override
    public Void execute(CommandContext context, IpAddress address) {
        logger.info("Deleting IP Adddress with addressId {}", address.ipAddressId);

        // secondary/additional ips do not need to be unbound
        if(address.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)) {
            UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
            unbindIpRequest.addressId = address.ipAddressId;
            unbindIpRequest.forceIfVmInaccessible = true;
            context.execute(UnbindIp.class, unbindIpRequest);
        }
        context.execute(ReleaseIp.class, address.ipAddressId);

        return null;
    }
}
