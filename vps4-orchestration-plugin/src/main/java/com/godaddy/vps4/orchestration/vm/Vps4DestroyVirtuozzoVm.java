package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4DestroyVirtuozzoVm",
        requestType = VmActionRequest.class,
        responseType = Vps4DestroyVirtuozzoVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyVirtuozzoVm extends Vps4DestroyVm {
    @Inject
    public Vps4DestroyVirtuozzoVm(ActionService actionService, NetworkService networkService) {
        super(actionService, networkService);
    }

    @Override
    protected void removeIpFromServer(IpAddress address) {
        // Optimized Hosting servers do not need to unbind and release IPs back to an IP Pool
    }
}
