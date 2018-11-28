package com.godaddy.vps4.orchestration.hfs.vm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.CreateVmStep;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmService;

public class WaitForVmAction implements Command<VmAction, VmAction> {

    private static final Logger logger = LoggerFactory.getLogger(WaitForVmAction.class);

    private final VmService vmService;

    @Inject
    public WaitForVmAction(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public VmAction execute(CommandContext context, VmAction hfsAction) {

        hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);

        int currentHfsTick = 1;
        // wait for VmAction to complete
        while (hfsAction.state == VmAction.Status.NEW
                || hfsAction.state == VmAction.Status.REQUESTED
                || hfsAction.state == VmAction.Status.IN_PROGRESS) {

            logger.debug("waiting for action to complete: {}", hfsAction);

            if (hfsAction.state == VmAction.Status.IN_PROGRESS) {
                // FIXME update the state of the action to include the vm
                //action.vm = vmService.getVm(hfsAction.vmId);
            }

            if (hfsAction.tickNum > currentHfsTick) {
                CreateVmStep newState = hfsTicks.get(hfsAction.tickNum);
                if (newState != null) {
                    // FIXME update the current step of the action
                    //action.step = newState;
                }
                currentHfsTick = hfsAction.tickNum;
            }

            context.sleep(2000);

            hfsAction = vmService.getVmAction(hfsAction.vmId, hfsAction.vmActionId);
        }
        if (!(hfsAction.state == VmAction.Status.COMPLETE)) {
            throw new RuntimeException(String.format("failed to complete VM action: %s", hfsAction));
        }
        return hfsAction;
    }

    private static final Map<Integer, CreateVmStep> hfsTicks = newHfsTicksMap();

    static Map<Integer, CreateVmStep> newHfsTicksMap() {
        Map<Integer, CreateVmStep> hfsTicksMap = new ConcurrentHashMap<>();
        hfsTicksMap.put(1, CreateVmStep.RequestingServer);
        hfsTicksMap.put(2, CreateVmStep.CreatingServer);
        hfsTicksMap.put(3, CreateVmStep.ConfiguringServer);
        return hfsTicksMap;
    }
}
