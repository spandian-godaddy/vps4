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
 * Block unprocessable actions for imported VMs
 */
public class ImportedVmFilter extends VmFilter {

    private static final Logger logger = LoggerFactory.getLogger(ImportedVmFilter.class);

    @Inject
    public ImportedVmFilter(VirtualMachineService virtualMachineService) {
        super(Arrays.asList("start",
                            "stop",
                            "restart",
                            "consoleUrl",
                            "snapshots",
                            "restore",
                            "reinstateAbuseSuspend",
                            "reinstateBillingSuspend",
                            "messaging",
                            "syncVmStatus",
                            "snapshotSchedules",
                            "zombie",
                            "revive",
                            "setPassword",
                            "",
                            "suspend",
                            "processSuspendMessage",
                            "reinstate",
                            "processReinstateMessage",
                            "customNote",
                            "customNotes",
                            "ohBackups",
                            "mergeShopper",
                            "setHostname"
                ),
              virtualMachineService);
    }

    @Override
    protected void doFilterSpecificFiltering(HttpServletRequest request, UUID vmId) {
        rejectRequestForImportedVm(request, vmId);
    }

    protected void rejectRequestForImportedVm(HttpServletRequest request, UUID vmId) {
        if (isImportedVm(vmId)) {
            String errorMsg = "Request not allowed for Imported VM";
            logger.info(errorMsg + String.format(", request: %s %s",
                    request.getMethod(), request.getRequestURI()));
            throw new Vps4Exception("BLOCKED_FOR_IMPORTED_VM", errorMsg);
        }
    }

    private boolean isImportedVm(UUID vmId) {
        return virtualMachineService.getImportedVm(vmId) != null;
    }

}
