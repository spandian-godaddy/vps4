package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4DestroyOHVm",
        requestType = Vps4DestroyVm.Request.class,
        responseType = Vps4DestroyOHVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyOHVm extends Vps4DestroyVm {

    @Inject
    public Vps4DestroyOHVm(ActionService actionService,
                           NetworkService networkService,
                           ShopperNotesService shopperNotesService) {
        super(actionService, networkService, shopperNotesService);
    }

    @Override
    protected void removeIpFromServer(IpAddress address) {
        // Optimized Hosting servers do not need to unbind and release IPs back to an IP Pool
    }
}
