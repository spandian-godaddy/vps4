package com.godaddy.vps4.orchestration.vm.rebuild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.vm.RebuildVm;
import com.godaddy.vps4.orchestration.vm.WaitForAndRecordVmAction;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.RebuildVmStep;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4RebuildOHVm",
        requestType= Vps4RebuildVm.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RebuildOHVm extends Vps4RebuildVm {
    private static final Logger logger = LoggerFactory.getLogger(Vps4RebuildOHVm.class);

    @Inject
    public Vps4RebuildOHVm(ActionService actionService, VirtualMachineService virtualMachineService,
                           NetworkService vps4NetworkService, VmUserService vmUserService,
                           CreditService creditService, PanoptaDataService panoptaDataService,
                           HfsVmTrackingRecordService hfsVmTrackingRecordService) {
        super(actionService, virtualMachineService, vps4NetworkService, vmUserService, creditService,
              panoptaDataService, hfsVmTrackingRecordService);
    }

    @Override
    protected long rebuildServer(long oldHfsVmId) throws Exception {
        try {
            long newHfsVmId = rebuildVm(oldHfsVmId);
            if (newHfsVmId == 0) {
                throw new Exception("HFS VM ID is not available. Expecting HFS VM ID.");
            }
            return newHfsVmId;
        } catch (RuntimeException e) {
            logger.info("Rebuild optimized hosting VM failed for VM ID: {}", oldHfsVmId);
            throw e;
        }
    }

    private long rebuildVm(long oldHfsVmId) {
        setStep(RebuildVmStep.RequestingServer);
        logger.info("Rebuild OH VM process");

        RebuildVm.Request rebuildRequest = rebuildHfsVmRequest(oldHfsVmId);
        VmAction vmAction = context.execute("RebuildOHVm", RebuildVm.class, rebuildRequest);

        updateServerDetails(request);

        context.execute(WaitForAndRecordVmAction.class, vmAction);
        updateHfsVmTrackingRecord(vmAction);

        return vmAction.vmId;
    }

    private RebuildVm.Request rebuildHfsVmRequest(long oldHfsVmId) {
        RebuildVm.Request rebuildRequest = new RebuildVm.Request();
        rebuildRequest.vmId = oldHfsVmId;
        rebuildRequest.hostname = request.rebuildVmInfo.hostname;
        rebuildRequest.image_name = request.rebuildVmInfo.image.hfsName;
        rebuildRequest.username = request.rebuildVmInfo.username;
        rebuildRequest.encryptedPassword = request.rebuildVmInfo.encryptedPassword;
        return rebuildRequest;
    }
}
