package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;

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
                           ShopperNotesService shopperNotesService,
                           SnapshotService snapshotService,
                           VirtualMachineService virtualMachineService) {
        super(actionService, networkService, shopperNotesService, snapshotService, virtualMachineService);
    }
}
