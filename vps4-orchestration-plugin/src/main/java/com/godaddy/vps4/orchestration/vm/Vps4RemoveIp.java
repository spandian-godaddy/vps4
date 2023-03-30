package com.godaddy.vps4.orchestration.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.network.RemoveIpFromBlacklist;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveIp implements Command<IpAddress, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveIp.class);

    @Override
    public Void execute(CommandContext context, IpAddress address) {
        logger.info("Deleting IP Adddress with addressId {}", address.addressId);

        // ips with 0 hfs address ids cannot be released/unbound in hfs
        if(address.hfsAddressId != 0) {
            // secondary/additional ips do not need to be unbound
            if(address.ipAddressType.equals(IpAddress.IpAddressType.PRIMARY)) {
                UnbindIp.Request unbindIpRequest = new UnbindIp.Request();
                unbindIpRequest.hfsAddressId = address.hfsAddressId;
                unbindIpRequest.forceIfVmInaccessible = true;
                context.execute(UnbindIp.class, unbindIpRequest);
            }
            context.execute(ReleaseIp.class, address.hfsAddressId);
        }
        context.execute(RemoveIpFromBlacklist.class, address.ipAddress);
        return null;
    }
}
