package com.godaddy.vps4.orchestration.vm.rebuild;

import com.godaddy.vps4.cdn.CdnDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildVm;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.RebuildVmStep;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4RebuildDedicated",
        requestType= Vps4RebuildVm.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebuildDedicated extends Vps4RebuildVm {
    private static final Logger logger = LoggerFactory.getLogger(Vps4RebuildDedicated.class);

    @Inject
    public Vps4RebuildDedicated(ActionService actionService, VirtualMachineService virtualMachineService,
                                NetworkService vps4NetworkService, VmUserService vmUserService,
                                CreditService creditService, PanoptaDataService panoptaDataService,
                                HfsVmTrackingRecordService hfsVmTrackingRecordService, NetworkService networkService,
                                ShopperNotesService shopperNotesService, CdnDataService cdnDataService) {
        super(actionService, virtualMachineService, vps4NetworkService, vmUserService, creditService,
              panoptaDataService, hfsVmTrackingRecordService, networkService, shopperNotesService, cdnDataService);
    }

    @Override
    protected long rebuildServer(long oldHfsVmId) throws Exception {
        try {
            long newHfsVmId = rebuildDedicated(oldHfsVmId);
            if (newHfsVmId == 0) {
                throw new Exception("HFS Vm ID is not available. Expecting HFS VM ID.");
            }
            return newHfsVmId;
        } catch (RuntimeException e) {
            logger.info("Rebuild Dedicated vm failed for dedicated vm id: {}", oldHfsVmId);
            throw e;
        }
    }

    private long rebuildDedicated(long oldHfsVmId) {
        setStep(RebuildVmStep.RequestingServer);
        logger.info("rebuild dedicated vm process");

        RebuildVm.Request rebuildDedRequest = rebuildHfsVmRequest(oldHfsVmId);
        VmAction vmAction = context.execute("RebuildDedicated", RebuildVm.class, rebuildDedRequest);

        updateServerDetails(request);

        context.execute(WaitForAndRecordVmAction.class, vmAction);
        updateHfsVmTrackingRecord(vmAction);

        return vmAction.vmId;
    }

    private RebuildVm.Request rebuildHfsVmRequest(long oldHfsVmId) {
        RebuildVm.Request rebuildDedRequest = new RebuildVm.Request();
        rebuildDedRequest.vmId = oldHfsVmId;
        rebuildDedRequest.hostname = request.rebuildVmInfo.hostname;
        rebuildDedRequest.image_name = request.rebuildVmInfo.image.hfsName;
        rebuildDedRequest.username = request.rebuildVmInfo.username;
        rebuildDedRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        return rebuildDedRequest;
    }

    @Override
    protected void configureMailRelay(long hfsVmId) {}
}
