package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4DestroyDedicated",
        requestType = Vps4DestroyVm.Request.class,
        responseType = Vps4DestroyDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyDedicated extends Vps4DestroyVm {

    @Inject
    public Vps4DestroyDedicated(ActionService actionService,
                                NetworkService networkService,
                                ShopperNotesService shopperNotesService,
                                SnapshotService snapshotService,
                                VirtualMachineService virtualMachineService,
                                CdnDataService cdnDataService) {
        super(actionService, networkService, shopperNotesService, snapshotService, virtualMachineService, cdnDataService);
    }

    @Override
    protected void removeIpFromServer(IpAddress address) {
        // Ded4 servers do not need to unbind and release IPs back to an IP Pool
    }
}
