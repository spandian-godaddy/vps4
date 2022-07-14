package com.godaddy.vps4.web.util;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Exception;

/**
 * If a VM API request includes a vmId, then verify that there are no active snapshot actions for that VM.
 * Return 400 response if snapshot action pending, otherwise allow the request to continue.
 */
public class VmActiveSnapshotFilter extends VmFilter {

    private static final Logger logger = LoggerFactory.getLogger(VmActiveSnapshotFilter.class);

    @Inject
    public VmActiveSnapshotFilter(VirtualMachineService virtualMachineService) {
        super(Arrays.asList("messaging", "outages", "alerts", "mergeShopper", "syncVmStatus", "cancel"), virtualMachineService);
    }

    protected void doFilterSpecificFiltering(HttpServletRequest request, UUID vmId) {
        validateNoActiveSnapshotAction(request, vmId);
    }

    private void validateNoActiveSnapshotAction(HttpServletRequest request, UUID vmId) throws Vps4Exception {
        Long snapshotActionId = getPendingSnapshotAction(vmId);
        if (snapshotActionId != null) {
            String errorMsg = "Request not allowed while snapshot action running";
            logger.info(errorMsg + String.format(", action: %s, request: %s %s",
                    snapshotActionId, request.getMethod(), request.getRequestURI()));
            throw new Vps4Exception("SNAPSHOT_ACTION_IN_PROGRESS", errorMsg);
        }
    }

    private Long getPendingSnapshotAction(UUID vmId) {
        return virtualMachineService.getPendingSnapshotActionIdByVmId(vmId);
    }

}
