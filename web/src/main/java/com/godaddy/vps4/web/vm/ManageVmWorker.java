package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class ManageVmWorker implements Runnable {

    protected static final Logger logger = LoggerFactory.getLogger(ManageVmWorker.class);
    protected ManageVmAction action;
    protected long vmId;
    protected VmService vmService;
    protected ActionService actionService;
    protected long totalWaitTime; // milliseconds
    protected long sleepIncrement; // milliseconds

    public ManageVmWorker(VmService vmService, ActionService actionService, long vmId, ManageVmAction action) {
        this(vmService, actionService, vmId, action, 300000, 2000);
    }

    public ManageVmWorker(VmService vmService, ActionService actionService, long vmId, ManageVmAction action, long totalWaitTime) {
        this(vmService, actionService, vmId, action, totalWaitTime, 2000);
    }

    public ManageVmWorker(VmService vmService, ActionService actionService, long vmId, ManageVmAction action, long totalWaitTime, long sleepIncrement) {
        this.vmService = vmService;
        this.actionService = actionService;
        this.vmId = vmId;
        this.action = action;
        this.totalWaitTime = totalWaitTime;
        this.sleepIncrement = sleepIncrement;
    }

    @Override
    public void run() {
        try {

            // call the hfs api to start, stop or restart vm
            ActionType actionType = action.getActionType();
            switch (actionType) {
                case START_VM:
                    performVmStart();
                    break;
                case STOP_VM:
                    performVmStop();
                    break;
                case RESTART_VM:
                    performVmRestart();
                    break;
                default:
                    logger.error("No action type specified on manage vm action object.");
                    throw new UnsupportedOperationException("No action specified.");
            }


        } catch (Exception ex) {
            String message = String.format("Failed to complete requested action %s on vm id: %d%n", action.getActionType().toString(), vmId);
            action.status = ActionStatus.ERROR;
            action.setMessage(message);
            logger.error(message, ex);
            actionService.failAction(action.getActionId(), "{}", message);
            throw new Vps4Exception("Exception in worker thread: " + this.getClass().getName(), message);
        }
    }

    public void performVmStart() {
        VmAction hfsVmAction = vmService.startVm(vmId);
        if(hfsVmAction == null) {
            throw new UnsupportedOperationException("No action specified.");
        }
        performVmAction(hfsVmAction);
    }

    public void performVmStop() {
        VmAction hfsVmAction = vmService.stopVm(vmId);
        if(hfsVmAction == null) {
            throw new UnsupportedOperationException("No action specified.");
        }
        performVmAction(hfsVmAction);
    }

    public void performVmRestart() {
        performVmStop();
        performVmStart();
    }

    public void performVmAction(VmAction hfsVmAction) {
        VmAction.Status hfsActionStatus = hfsVmAction.state;
        long timeoutAt = System.currentTimeMillis() + totalWaitTime;

        while ((VmAction.Status.IN_PROGRESS == hfsActionStatus
                || VmAction.Status.REQUESTED == hfsActionStatus)
                && (timeoutAt > System.currentTimeMillis())) {

            logger.info("Thread is waiting on VM action to complete: {}", hfsVmAction);
            logger.debug("Current time: " + System.currentTimeMillis() + " Timeout At: " + timeoutAt);
            logger.info(String.format("Time left to timeout: %d seconds", TimeUnit.MILLISECONDS.toSeconds(timeoutAt - System.currentTimeMillis())));

            // wait and allow the hfs vm service to finish its work
            try {
                TimeUnit.MILLISECONDS.sleep(sleepIncrement);
            } catch (InterruptedException ex) {
                String message = "Worker interrupted while waiting for task completion.";
                logger.warn(message, ex);
                action.status = ActionStatus.ERROR;
                action.setMessage(message);
                actionService.failAction(action.getActionId(), "{}", message);
            }

            // check if hfs has finished its work
            hfsVmAction = vmService.getVmAction(vmId, hfsVmAction.vmActionId);
            hfsActionStatus = hfsVmAction.state;
        }

        if (VmAction.Status.COMPLETE == hfsActionStatus) {
            // if successful, update status on action to complete.
            String message = String.format("Action completed on VM id: %d %s", vmId, hfsVmAction.toString());
            action.status = ActionStatus.COMPLETE;
            action.setMessage(message);
            actionService.completeAction(action.getActionId(), "{}", message);
            logger.info(message);
        } else if (timeoutAt <= System.currentTimeMillis()) {
            String message = String.format(" Worker Timeout occurred. Could not complete action %s on vm id: %d%n ", hfsVmAction.toString(), vmId);
            action.status = ActionStatus.ERROR;
            action.setMessage(message);
            actionService.failAction(action.getActionId(), "{}", message);
            logger.warn(message);
        } else {
            String message = String.format("Could not complete action %s on vm id: %d%n ", hfsVmAction.toString(), vmId);
            action.status = ActionStatus.ERROR;
            action.setMessage(message);
            actionService.failAction(action.getActionId(), "{}", message);
            logger.warn(message);
        }
    }

}
